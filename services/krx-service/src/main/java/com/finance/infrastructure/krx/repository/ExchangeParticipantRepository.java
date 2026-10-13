package com.finance.infrastructure.krx.repository;

import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.krx.domain.participant.ExchangeParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeParticipantRepository extends JpaRepository<ExchangeParticipant, Long> {

    Optional<ExchangeParticipant> findByInstitutionId(InstitutionId institutionId);
}
