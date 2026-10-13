package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.account.StockHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StockHoldingRepository extends JpaRepository<StockHolding, Long> {

    Optional<StockHolding> findByAccount_IdAndSecurity_SecurityCode(
            Long accountId,
            String securityCode
    );

    List<StockHolding> findAllByAccount_IdOrderBySecurity_SecurityCodeAsc(Long accountId);
}
