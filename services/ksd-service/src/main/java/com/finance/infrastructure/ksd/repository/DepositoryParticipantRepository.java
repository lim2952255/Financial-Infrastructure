package com.finance.infrastructure.ksd.repository;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.ksd.domain.participant.DepositoryParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepositoryParticipantRepository extends JpaRepository<DepositoryParticipant, Long> {

    Optional<DepositoryParticipant> findByInstitutionId(InstitutionId institutionId);
}
