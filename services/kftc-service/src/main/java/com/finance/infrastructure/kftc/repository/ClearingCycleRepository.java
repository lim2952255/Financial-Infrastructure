package com.finance.infrastructure.kftc.repository;

import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ClearingCycleRepository extends JpaRepository<ClearingCycle, Long> {

    @EntityGraph(attributePaths = "positions")
    List<ClearingCycle> findAllByOrderByIdAsc();

    Optional<ClearingCycle> findByCycleId(String cycleId);
}
