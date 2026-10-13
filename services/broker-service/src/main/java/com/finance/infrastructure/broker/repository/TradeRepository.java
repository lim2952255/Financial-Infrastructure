package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.trade.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    Optional<Trade> findByTradeId(String tradeId);

    List<Trade> findAllByOrderIdOrderByCreatedAtAsc(Long orderId);
}
