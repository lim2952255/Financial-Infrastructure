package com.finance.infrastructure.ksd.repository;

import com.finance.infrastructure.ksd.domain.security.DepositorySecurity;
import com.finance.infrastructure.ksd.domain.security.TradingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepositorySecurityRepository extends JpaRepository<DepositorySecurity, Long> {

    Optional<DepositorySecurity> findBySecurityCode(String securityCode);

    List<DepositorySecurity> findAllByTradingStatusOrderBySecurityCodeAsc(TradingStatus tradingStatus);
}
