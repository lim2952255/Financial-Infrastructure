package com.finance.infrastructure.bok.config;

import com.finance.infrastructure.bok.domain.account.SettlementAccount;
import com.finance.infrastructure.bok.domain.participant.SettlementParticipant;
import com.finance.infrastructure.bok.repository.SettlementAccountRepository;
import com.finance.infrastructure.bok.repository.SettlementParticipantRepository;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
// Bank A/B/C의 BOK 참가자와 당좌예금 계좌를 생성한다.
public class BokDemoDataSeeder implements ApplicationRunner {

    // 각 은행의 초기 당좌예금
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000000");
    // BOK 참가은행 설정
    private static final DemoParticipant[] DEMO_PARTICIPANTS = {
            new DemoParticipant("BANK_A", "Bank A", "BOK-A"),
            new DemoParticipant("BANK_B", "Bank B", "BOK-B"),
            new DemoParticipant("BANK_C", "Bank C", "BOK-C")
    };

    private final BokDemoProperties properties;
    private final SettlementParticipantRepository participantRepository;
    private final SettlementAccountRepository accountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seedEnabled()) {
            return;
        }

        // BOK 참가은행들을 레포지터리에 등록하고, 각 참가은행들의 당좌예금 계좌를 등록한다.
        for (DemoParticipant demo : DEMO_PARTICIPANTS) {
            InstitutionId institutionId = InstitutionId.of(demo.institutionId());
            SettlementParticipant participant = participantRepository
                    .findByInstitutionId(institutionId)
                    .orElseGet(() -> participantRepository.save(
                            new SettlementParticipant(institutionId, demo.name())
                    ));
            if (!accountRepository.existsByParticipantInstitutionId(institutionId)) {
                SettlementAccount account = new SettlementAccount(
                        demo.accountNumber(),
                        participant
                );
                account.credit(Money.of(INITIAL_BALANCE));
                accountRepository.save(account);
            }
        }
    }

    private record DemoParticipant(String institutionId, String name, String accountNumber) {
    }
}
