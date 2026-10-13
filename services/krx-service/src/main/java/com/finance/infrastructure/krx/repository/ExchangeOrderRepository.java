package com.finance.infrastructure.krx.repository;

import com.finance.infrastructure.krx.domain.order.ExchangeOrder;
import com.finance.infrastructure.krx.domain.order.ExchangeOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExchangeOrderRepository extends JpaRepository<ExchangeOrder, Long> {

    Optional<ExchangeOrder> findByExchangeOrderId(String exchangeOrderId);

    Optional<ExchangeOrder> findByParticipantIdAndBrokerOrderId(Long participantId, String brokerOrderId);

    List<ExchangeOrder> findAllBySecurityIdAndStatusInOrderByReceivedSequence(
            Long securityId,
            List<ExchangeOrderStatus> statuses
    );
}
