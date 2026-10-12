package com.finance.infrastructure.kftc.config;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.kftc.domain.participant.ClearingParticipant;
import com.finance.infrastructure.kftc.repository.ClearingParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
// Bank A/B/C를 KFTC 청산 참가자로 등록한다.
public class KftcDemoDataSeeder implements ApplicationRunner {

    // 청산 참가 은행 명단
    private static final String[] DEMO_INSTITUTIONS = {"BANK_A", "BANK_B", "BANK_C"};

    // 청산 참가자들을 등록할지 여부를 담은 레코드
    private final KftcDemoProperties properties;
    private final ClearingParticipantRepository participantRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seedEnabled()) {
            return;
        }
        // 이미 해당 청산 참가 은행들이 repository에 저장되어 있으면 중복으로 저장하지 않는다.
        for (String value : DEMO_INSTITUTIONS) {
            InstitutionId institutionId = InstitutionId.of(value);
            if (participantRepository.findByInstitutionId(institutionId).isEmpty()) {
                participantRepository.save(new ClearingParticipant(institutionId));
            }
        }
    }
}
