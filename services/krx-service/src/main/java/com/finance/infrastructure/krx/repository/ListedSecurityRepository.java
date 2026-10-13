package com.finance.infrastructure.krx.repository;

import com.finance.infrastructure.krx.domain.security.ListedSecurity;
import com.finance.infrastructure.krx.domain.security.TradingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListedSecurityRepository extends JpaRepository<ListedSecurity, Long> {

    Optional<ListedSecurity> findBySecurityCode(String securityCode);

    List<ListedSecurity> findAllByTradingStatusOrderBySecurityCodeAsc(TradingStatus tradingStatus);
}
