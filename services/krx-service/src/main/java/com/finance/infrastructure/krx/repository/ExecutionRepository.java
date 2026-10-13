package com.finance.infrastructure.krx.repository;

import com.finance.infrastructure.krx.domain.trade.Execution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    Optional<Execution> findByExecutionId(String executionId);

    List<Execution> findAllByTradeDateAndClearingCycleIsNullOrderByExecutedAtAsc(LocalDate tradeDate);

    List<Execution> findAllByClearingCycleIdOrderByExecutedAtAsc(Long clearingCycleId);
}
