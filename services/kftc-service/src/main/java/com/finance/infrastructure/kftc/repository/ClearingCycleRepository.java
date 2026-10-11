package com.finance.infrastructure.kftc.repository;

import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClearingCycleRepository extends JpaRepository<ClearingCycle, Long> {

    Optional<ClearingCycle> findByCycleId(String cycleId);
}
