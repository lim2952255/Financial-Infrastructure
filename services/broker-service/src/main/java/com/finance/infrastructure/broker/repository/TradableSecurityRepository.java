package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.security.TradableSecurity;
import com.finance.infrastructure.broker.domain.security.TradingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradableSecurityRepository extends JpaRepository<TradableSecurity, Long> {

    Optional<TradableSecurity> findBySecurityCode(String securityCode);

    List<TradableSecurity> findAllByTradingStatusOrderBySecurityCodeAsc(TradingStatus tradingStatus);
}
