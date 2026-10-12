package com.finance.infrastructure.bok.service.settlement;

import com.finance.infrastructure.bok.domain.account.SettlementAccount;
import com.finance.infrastructure.bok.domain.participant.SettlementParticipant;
import com.finance.infrastructure.bok.repository.SettlementAccountRepository;
import com.finance.infrastructure.bok.repository.SettlementBatchRepository;
import com.finance.infrastructure.bok.repository.SettlementLedgerRepository;
import com.finance.infrastructure.bok.repository.SettlementParticipantRepository;
import com.finance.infrastructure.bok.support.MySqlIntegrationTestSupport;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// BOK가 기관별 순포지션을 실제 당좌예금 잔액과 원장에 반영하는지 검증한다.
@SpringBootTest // 스프링부트의 전체 Application Context를 띄우는 통합테스트 설정
class SettlementServiceIntegrationTest extends MySqlIntegrationTestSupport {

    @Autowired
    private SettlementService settlementService; // KFTC로부터 받은 결제요청정보를 기반으로 각 기관의 당좌예금을 결제하는 서비스

    @Autowired
    private SettlementParticipantRepository participantRepository;

    @Autowired
    private SettlementAccountRepository accountRepository;

    @Autowired
    private SettlementBatchRepository batchRepository;

    @Autowired
    private SettlementLedgerRepository ledgerRepository;

    // BOK에서 관리하는 BankA와 BankB의 당좌예금 계좌
    private SettlementAccount bankAAccount;
    private SettlementAccount bankBAccount;

    // 매 테스트를 수행하기 전에 MySQL Database에 저장된 데이터들을 초기화한다.
    @BeforeEach
    void setUp() {
        ledgerRepository.deleteAll();
        batchRepository.deleteAll();
        accountRepository.deleteAll();
        participantRepository.deleteAll();

        // 테스트에 사용할 BankA와 BankB 금융기관정보를 등록한다.
        SettlementParticipant bankA = participantRepository.save(
                new SettlementParticipant(InstitutionId.of("BANK_A"), "Bank A")
        );
        SettlementParticipant bankB = participantRepository.save(
                new SettlementParticipant(InstitutionId.of("BANK_B"), "Bank B")
        );

        // 등록한 BankA와 BankB의 금융기관정보를 바탕으로 BankA와 BankB의 당좌예금 계좌를 개설한다.
        bankAAccount = new SettlementAccount("BOK-A-001", bankA);
        bankAAccount.credit(Money.of(new BigDecimal("100000")));
        bankAAccount = accountRepository.save(bankAAccount);
        bankBAccount = accountRepository.save(new SettlementAccount("BOK-B-001", bankB));
    }

    // 결제 정상 처리 및 멱등성 보장 테스트
    @Test
    void settlesNetPositionsAndDoesNotApplyTheSameBatchTwice() {
        // 결제요청정보를 생성한다. (BankA의 순포지션: -30000, BankB의 순포지션: +30000)
        SettlementCommand command = command(
                "batch-001",
                new BigDecimal("-30000"),
                new BigDecimal("30000")
        );

        // 동일한 SettlementCommand를 두번 요청한다. 이 경우 batchId를 기반으로 두번째 SettlementCommand에 대해서는 한번 더 처리하는 것이 아니라 기존 결제결과를 리턴함으로서 멱등성을 보장한다.
        SettlementResult first = settlementService.settle(command);
        SettlementResult second = settlementService.settle(command);

        // 첫번째 SettlementCommand는 SETTLED가 되었는지, 그리고 두번째 SettlementCommand는 처리하지 않고, 첫번째 SettlementCommand만 처리되었는지를 검사한다.
        assertThat(first.status()).isEqualTo("SETTLED");
        assertThat(second.batchId()).isEqualTo(first.batchId());
        assertThat(batchRepository.count()).isEqualTo(1);
        assertThat(ledgerRepository.count()).isEqualTo(2);
        // BankA와 BankB에 정상적으로 결제가 수행되어 당좌예금이 update되었는지를 검사한다.
        assertThat(accountRepository.findById(bankAAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("70000")));
        assertThat(accountRepository.findById(bankBAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("30000")));
    }

    // 결제를 시도하는데 만약 기관의 당좌예금에 당좌예금이 부족한 경우에는 전체를 롤백하고 결제요청을 reject한다.
    @Test
    void rollsBackAllAccountsWhenASettlementAccountHasInsufficientBalance() {
        // 결제요청정보를 생성한다. (BankA의 순포지션: -100001, BankB의 순포지션: +100001)
        SettlementCommand command = command(
                "batch-002",
                new BigDecimal("-100001"),
                new BigDecimal("100001")
        );
        // 이 경우 BankA의 당좌예금 계좌에는 100000원밖에 없기 때문에 잔액이 부족해서 결제를 처리할때 예외가 발생한다.
        assertThatThrownBy(() -> settlementService.settle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("insufficient settlement account balance");

        // 결제가 실패하면 모든 결제정보가 롤백되어야 한다.
        assertThat(batchRepository.count()).isZero();
        assertThat(ledgerRepository.count()).isZero();
        assertThat(accountRepository.findById(bankAAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("100000")));
        assertThat(accountRepository.findById(bankBAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.zero());
    }

    // KFTC가 보냈다고 가정하는 결제요청 SettlementCommand
    private static SettlementCommand command(
            String batchId,
            BigDecimal bankANetAmount,
            BigDecimal bankBNetAmount
    ) {
        return new SettlementCommand(
                batchId,
                "cycle-001",
                List.of(
                        new SettlementCommand.PositionCommand(
                                InstitutionId.of("BANK_A"),
                                Money.of(bankANetAmount)
                        ),
                        new SettlementCommand.PositionCommand(
                                InstitutionId.of("BANK_B"),
                                Money.of(bankBNetAmount)
                        )
                )
        );
    }
}
