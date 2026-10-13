package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.order.BrokerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BrokerOrderRepository extends JpaRepository<BrokerOrder, Long> {

    Optional<BrokerOrder> findByOrderId(String orderId);

    Optional<BrokerOrder> findByExchangeOrderId(String exchangeOrderId);

    List<BrokerOrder> findAllByAccountIdOrderByCreatedAtDesc(Long accountId);
}
