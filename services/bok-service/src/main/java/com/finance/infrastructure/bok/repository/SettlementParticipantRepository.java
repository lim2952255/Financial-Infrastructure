package com.finance.infrastructure.bok.repository;

import com.finance.infrastructure.bok.domain.participant.SettlementParticipant;
import com.finance.infrastructure.common.institution.InstitutionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface SettlementParticipantRepository extends JpaRepository<SettlementParticipant, Long> {

    List<SettlementParticipant> findAllByOrderByIdAsc();

    Optional<SettlementParticipant> findByInstitutionId(InstitutionId institutionId);
}
