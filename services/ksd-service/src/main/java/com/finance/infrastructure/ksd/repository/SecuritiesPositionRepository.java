package com.finance.infrastructure.ksd.repository;

import com.finance.infrastructure.ksd.domain.account.SecuritiesPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecuritiesPositionRepository extends JpaRepository<SecuritiesPosition, Long> {

    Optional<SecuritiesPosition> findByAccount_IdAndSecurity_SecurityCode(
            Long accountId,
            String securityCode
    );

    List<SecuritiesPosition> findAllByAccount_IdOrderBySecurity_SecurityCodeAsc(Long accountId);
}
