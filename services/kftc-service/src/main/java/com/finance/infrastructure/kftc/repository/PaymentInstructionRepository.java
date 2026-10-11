package com.finance.infrastructure.kftc.repository;

import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.participant.ClearingParticipant;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstruction;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstructionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PaymentInstructionRepository extends JpaRepository<PaymentInstruction, Long> {

    Optional<PaymentInstruction> findBySenderParticipantAndRequestId(
            ClearingParticipant senderParticipant,
            String requestId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<PaymentInstruction> findAllByStatusAndClearingCycleIsNullOrderByIdAsc(
            PaymentInstructionStatus status
    );

    List<PaymentInstruction> findAllByClearingCycleOrderByIdAsc(ClearingCycle clearingCycle);
}
