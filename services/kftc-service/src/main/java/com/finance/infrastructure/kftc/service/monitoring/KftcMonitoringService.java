package com.finance.infrastructure.kftc.service.monitoring;

import com.finance.infrastructure.kftc.controller.monitoring.KftcMonitoringResponse;
import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.clearing.NetPosition;
import com.finance.infrastructure.kftc.domain.participant.ClearingParticipant;
import com.finance.infrastructure.kftc.domain.payment.PaymentInstruction;
import com.finance.infrastructure.kftc.domain.settlement.SettlementSubmission;
import com.finance.infrastructure.kftc.repository.ClearingCycleRepository;
import com.finance.infrastructure.kftc.repository.ClearingParticipantRepository;
import com.finance.infrastructure.kftc.repository.PaymentInstructionRepository;
import com.finance.infrastructure.kftc.repository.SettlementSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
// KFTC 참가자·지급지시·청산·결제 제출 데이터를 record에 담아 대시보드에 응답하는 서비스
public class KftcMonitoringService {

    private final ClearingParticipantRepository participantRepository;
    private final PaymentInstructionRepository instructionRepository;
    private final ClearingCycleRepository cycleRepository;
    private final SettlementSubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public KftcMonitoringResponse getMonitoringSnapshot() {
        return new KftcMonitoringResponse(
                participantRepository.findAllByOrderByIdAsc().stream().map(this::participant).toList(),
                instructionRepository.findAllForMonitoring().stream().map(this::instruction).toList(),
                cycleRepository.findAllByOrderByIdAsc().stream().map(this::cycle).toList(),
                submissionRepository.findAllForMonitoring().stream().map(this::submission).toList()
        );
    }

    private KftcMonitoringResponse.Participant participant(ClearingParticipant participant) {
        return new KftcMonitoringResponse.Participant(
                participant.getInstitutionId().value(),
                participant.getStatus().name(),
                timestamp(participant.getCreatedAt())
        );
    }

    private KftcMonitoringResponse.PaymentInstruction instruction(PaymentInstruction instruction) {
        return new KftcMonitoringResponse.PaymentInstruction(
                instruction.getInstructionId(),
                instruction.getRequestId(),
                instruction.getSenderParticipant().getInstitutionId().value(),
                instruction.getSenderAccountNumber(),
                instruction.getReceiverParticipant().getInstitutionId().value(),
                instruction.getReceiverAccountNumber(),
                instruction.amount().amount(),
                instruction.getStatus().name(),
                instruction.getClearingCycle() == null
                        ? null : instruction.getClearingCycle().getCycleId(),
                timestamp(instruction.getReceivedAt()),
                timestamp(instruction.getForwardedAt()),
                timestamp(instruction.getReceiverCreditedAt()),
                timestamp(instruction.getSettledAt()),
                instruction.getFailureReason()
        );
    }

    private KftcMonitoringResponse.ClearingCycle cycle(ClearingCycle cycle) {
        return new KftcMonitoringResponse.ClearingCycle(
                cycle.getCycleId(),
                cycle.getStatus().name(),
                cycle.getPositions().stream().map(this::position).toList(),
                timestamp(cycle.getOpenedAt()),
                timestamp(cycle.getClosedAt()),
                timestamp(cycle.getCalculatedAt()),
                timestamp(cycle.getSettledAt()),
                cycle.getFailureReason()
        );
    }

    private KftcMonitoringResponse.Position position(NetPosition position) {
        return new KftcMonitoringResponse.Position(
                position.institutionId().value(),
                position.netAmount().amount()
        );
    }

    private KftcMonitoringResponse.SettlementSubmission submission(
            SettlementSubmission submission
    ) {
        return new KftcMonitoringResponse.SettlementSubmission(
                submission.getSubmissionId(),
                submission.getClearingCycle().getCycleId(),
                submission.getStatus().name(),
                timestamp(submission.getCreatedAt()),
                timestamp(submission.getSubmittedAt()),
                timestamp(submission.getSettledAt()),
                submission.getRejectionReason()
        );
    }

    private static OffsetDateTime timestamp(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
