package com.finance.infrastructure.bank.service.transfer.interbank;

import com.finance.infrastructure.bank.domain.transaction.BankTransaction;
import com.finance.infrastructure.bank.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 한 청산주기에 포함된 은행 간 거래들의 BOK 최종 결제 완료를 일괄 반영한다.
@Service
@RequiredArgsConstructor
public class InterbankSettlementService {

    private final BankTransactionRepository transactionRepository;

    @Transactional
    public void confirm(InterbankSettlementCommand command) {
        // InterbankSettlementCommand에 포함된 instructionId 목록을 기반으로 모든 Transaction을 조회한다.
        List<BankTransaction> transactions = transactionRepository
                .findAllByPaymentInstructionIdInForUpdate(command.instructionIds());

        // command에 포함된 instructionId 목록중 실제 은행의 Transaction repository에 저장되어 있는 instructionId 목록 저장
        Set<String> foundInstructionIds = new HashSet<>();
        transactions.forEach(transaction ->
                foundInstructionIds.add(transaction.getPaymentInstructionId())
        );
        // command에 포함된 instructionId 목록중 실제 은행의 Transaction repository에 저장되어 있지 않은 instructionId 목록 저장 (오류)
        List<String> missingInstructionIds = command.instructionIds().stream()
                .filter(instructionId -> !foundInstructionIds.contains(instructionId))
                .toList();
        // 만약 missingInstructionIds가 비어있지 않다면, 결제처리에 활용된 instructionId가 실제 금융기관에는 존재하지 않는다는 의미이므로 기관간 정보 불일치 문제가 발생한 것이다.
        if (!missingInstructionIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "bank transactions not found for instructions: " + missingInstructionIds
            );
        }

        // 결제배치에 포함된 모든 transaction들을 순차적으로 조회하면서, 각 transaction이 포함된 청산주기, BOK의 결제배치 정보를 반영하면서 transaction에 결제완료 정보를 기록한다.
        transactions.forEach(transaction -> transaction.confirmInterbankSettlement(
                command.clearingCycleId(),
                command.settlementBatchId()
        ));
    }
}
