package com.finance.infrastructure.krx.repository;

import com.finance.infrastructure.krx.domain.clearing.ClearingCycle;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClearingCycleRepository extends JpaRepository<ClearingCycle, Long> {

    Optional<ClearingCycle> findByCycleId(String cycleId);

    Optional<ClearingCycle> findByTradeDateAndSettlementDate(LocalDate tradeDate, LocalDate settlementDate);

    @EntityGraph(attributePaths = {"securitiesPositions", "cashPositions"})
    List<ClearingCycle> findAllByOrderByTradeDateDesc();
}
