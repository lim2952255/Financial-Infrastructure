package com.finance.infrastructure.bok.repository;

import com.finance.infrastructure.bok.domain.account.SettlementAccount;
import com.finance.infrastructure.common.institution.InstitutionId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SettlementAccountRepository extends JpaRepository<SettlementAccount, Long> {

    // 여러 기관을 결제할 때 서비스가 기관 ID 순서대로 호출하여 잠금 순서를 일정하게 유지한다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select account
            from SettlementAccount account
            join fetch account.participant participant
            where participant.institutionId = :institutionId
            """)
    Optional<SettlementAccount> findByInstitutionIdForUpdate(
            @Param("institutionId") InstitutionId institutionId
    );
}
