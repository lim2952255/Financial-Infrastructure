package com.finance.infrastructure.bok.service.settlement;

import com.finance.infrastructure.bok.domain.account.SettlementAccount;
import com.finance.infrastructure.bok.domain.ledger.SettlementLedger;
import com.finance.infrastructure.bok.domain.ledger.SettlementLedgerEntryType;
import com.finance.infrastructure.bok.domain.settlement.SettlementBatch;
import com.finance.infrastructure.bok.domain.settlement.SettlementPosition;
import com.finance.infrastructure.bok.repository.SettlementAccountRepository;
import com.finance.infrastructure.bok.repository.SettlementBatchRepository;
import com.finance.infrastructure.bok.repository.SettlementLedgerRepository;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

// BOK 당좌예금의 차변·대변 처리와 원장 기록을 하나의 트랜잭션으로 수행한다.
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementBatchRepository batchRepository;
    private final SettlementAccountRepository accountRepository;
    private final SettlementLedgerRepository ledgerRepository;

    // KFTC로부터 받은 결제 요청정보를 기반으로 실제 결제를 수행한다.
    @Transactional
    public SettlementResult settle(SettlementCommand command) {

        // 기관별 순포지션정보를 바탕으로 SettlementPosition 객체를 생성한다.(기관별 순포지션정보를 담은 객체)
        List<SettlementPosition> positions = command.positions().stream()
                .map(position -> new SettlementPosition(
                        position.institutionId(),
                        position.netAmount()
                ))
                .toList();

        // KFTC에서 요청한 batchId가 이미 batchRepository에 존재하는지를 검사한다 (멱등성 보장)
        // batchId를 BOK에서 생성하는 것이 아니라, KFTC에서 생성해서 BOK에 전달하는 이유는, 동일 결제요청을 KFTC가 보냈을때, BOK가 같은 요청임에도 서로다른 batchId를 할당할 수 있기 때문에, KFTC가 생서아도록 하여 멱등성을 보장한다.
        SettlementBatch existing = batchRepository.findByBatchId(command.batchId())
                .orElse(null);
        if (existing != null) {
            // 동일한 batchId가 이미 저장되어 있는 경우에, 이전에 처리중이던 결제요청인지, 아니면 새로운 결제요청인데 우연히 batchId가 중복된것인지를 검사한다.
            requireSameBatch(existing, command.clearingCycleId(), positions);
            return SettlementResult.from(existing);
        }

        // 이번에 결제를 수행할 새로운 결제배치(SettlementBatch)를 생성 후 저장한다.
        SettlementBatch batch = batchRepository.save(new SettlementBatch(
                command.batchId(),
                command.clearingCycleId(),
                positions
        ));
        // 결제를 수행할 수 있도록, 결제배치의 상태를 VALIDATE로 변경한다.
        batch.validate();

        // 모든 계좌를 먼저 기관ID의 오름차순 순서로 잠가 데드락을 방지한다.
        // 여러 결제 배치가 동시에 결제를 수행하다보면 결제배치1: A->B, 결제배치2: B->A 순서로 lock을 걸게 되면 데드락이 발생할 수 있고 이를 방지한다.
        Map<InstitutionId, SettlementAccount> accounts = lockAccounts(positions);
        // 기관마다 결제를 수행하고, 해당 원장정보를 저장한다.
        List<SettlementLedger> ledgerEntries = new ArrayList<>();

        // SettlementPosition을 하나씩 조회한다.
        for (SettlementPosition position : positions) {
            // 각 기관의 계좌를 조회한다.
            SettlementAccount account = accounts.get(position.institutionId());
            Money netAmount = position.netAmount(); // 결제할 금액
            Money absoluteAmount = Money.of(netAmount.amount().abs());

            // 차변
            if (netAmount.amount().signum() < 0) {
                Money balanceAfter = account.debit(absoluteAmount); // 기관의 당좌예금 계좌에서 당좌예금을 차감한다.
                // 차변 원장을 기록한다.
                ledgerEntries.add(new SettlementLedger(
                        batch,
                        account,
                        SettlementLedgerEntryType.DEBIT,
                        absoluteAmount,
                        balanceAfter
                ));
            // 대변
            } else {
                Money balanceAfter = account.credit(absoluteAmount); // 기관의 당좌예금 계좌에서 당좌예금을 증액한다.
                // 대변 원장을 기록한다.
                ledgerEntries.add(new SettlementLedger(
                        batch,
                        account,
                        SettlementLedgerEntryType.CREDIT,
                        absoluteAmount,
                        balanceAfter
                ));
            }
        }
        // 모든 기관의 원장기록을 저장한다.
        ledgerRepository.saveAll(ledgerEntries);
        // 결제배치의 상태를 SETTLE, 즉 결제완료 상태로 변경한다.
        batch.settle();
        return SettlementResult.from(batch);
    }

    // 모든 계좌를 먼저 기관ID의 오름차순 순서로 잠가 데드락을 방지한다.
    // 여러 결제 배치가 동시에 결제를 수행하다보면 결제배치1: A->B, 결제배치2: B->A 순서로 lock을 걸게 되면 데드락이 발생할 수 있고 이를 방지한다.
    private Map<InstitutionId, SettlementAccount> lockAccounts(
            List<SettlementPosition> positions
    ) {
        // 결제배치에 포함된 기관들의 institutionId를 기반으로 오름차순 정렬을 한다.
        List<InstitutionId> institutionIds = positions.stream()
                .map(SettlementPosition::institutionId)
                .sorted(Comparator.comparing(InstitutionId::value))
                .toList();

        Map<InstitutionId, SettlementAccount> accounts = new HashMap<>();
        // institutionId의 오름차순으로 각 기관의 당좌예금 계좌를 lock을 획득한 채로 조회한다.
        for (InstitutionId institutionId : institutionIds) {
            SettlementAccount account = accountRepository
                    .findByInstitutionIdForUpdate(institutionId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "settlement account not found: " + institutionId.value()
                    ));
            accounts.put(institutionId, account);
        }
        return accounts;
    }

    // Batch Id가 중복되었을때, 이전 결제배치요청을 한번 더 요청한건지, 다른 결제배치 요청인데 우연히 Batch Id가 중복된 것인지를 검사한다.
    private static void requireSameBatch(
            SettlementBatch existing,
            String clearingCycleId,
            List<SettlementPosition> positions
    ) {
        boolean sameBatch = existing.getClearingCycleId().equals(clearingCycleId)
                && new HashSet<>(existing.getPositions()).equals(new HashSet<>(positions));
        if (!sameBatch) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "batch id was already used for a different settlement request"
            );
        }
    }
}
