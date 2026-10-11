package com.finance.infrastructure.kftc.repository;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.kftc.domain.participant.ClearingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClearingParticipantRepository extends JpaRepository<ClearingParticipant, Long> {

    Optional<ClearingParticipant> findByInstitutionId(InstitutionId institutionId);
}
