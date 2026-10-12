package com.finance.infrastructure.bok.service.monitoring;

import com.finance.infrastructure.bok.controller.monitoring.BokMonitoringResponse;
import com.finance.infrastructure.bok.domain.account.SettlementAccount;
import com.finance.infrastructure.bok.domain.ledger.SettlementLedger;
import com.finance.infrastructure.bok.domain.participant.SettlementParticipant;
import com.finance.infrastructure.bok.domain.settlement.SettlementBatch;
import com.finance.infrastructure.bok.domain.settlement.SettlementPosition;
import com.finance.infrastructure.bok.repository.SettlementAccountRepository;
import com.finance.infrastructure.bok.repository.SettlementBatchRepository;
import com.finance.infrastructure.bok.repository.SettlementLedgerRepository;
import com.finance.infrastructure.bok.repository.SettlementParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
// BOK의 참가은행, 참가은행들의 당좌예금 계좌, 결재 배치, 원장기록등을 record로 생성하는 서비스
public class BokMonitoringService {

    private final SettlementParticipantRepository participantRepository;
    private final SettlementAccountRepository accountRepository;
    private final SettlementBatchRepository batchRepository;
    private final SettlementLedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public BokMonitoringResponse getMonitoringSnapshot() {
        return new BokMonitoringResponse(
                participantRepository.findAllByOrderByIdAsc().stream().map(this::participant).toList(),
                accountRepository.findAllForMonitoring().stream().map(this::account).toList(),
                batchRepository.findAllByOrderByIdAsc().stream().map(this::batch).toList(),
                ledgerRepository.findAllForMonitoring().stream().map(this::ledgerEntry).toList()
        );
    }

    private BokMonitoringResponse.Participant participant(SettlementParticipant participant) {
        return new BokMonitoringResponse.Participant(
                participant.getInstitutionId().value(),
                participant.getName(),
                participant.getStatus().name(),
                timestamp(participant.getCreatedAt())
        );
    }

    private BokMonitoringResponse.SettlementAccount account(SettlementAccount account) {
        return new BokMonitoringResponse.SettlementAccount(
                account.getAccountNumber(),
                account.getParticipant().getInstitutionId().value(),
                account.balance().amount(),
                account.getStatus().name(),
                timestamp(account.getCreatedAt())
        );
    }

    private BokMonitoringResponse.SettlementBatch batch(SettlementBatch batch) {
        return new BokMonitoringResponse.SettlementBatch(
                batch.getBatchId(),
                batch.getClearingCycleId(),
                batch.getStatus().name(),
                batch.getPositions().stream().map(this::position).toList(),
                timestamp(batch.getReceivedAt()),
                timestamp(batch.getSettledAt()),
                batch.getRejectionReason()
        );
    }

    private BokMonitoringResponse.Position position(SettlementPosition position) {
        return new BokMonitoringResponse.Position(
                position.institutionId().value(),
                position.netAmount().amount()
        );
    }

    private BokMonitoringResponse.LedgerEntry ledgerEntry(SettlementLedger ledger) {
        return new BokMonitoringResponse.LedgerEntry(
                ledger.getId(),
                ledger.getSettlementBatch().getBatchId(),
                ledger.getSettlementBatch().getClearingCycleId(),
                ledger.getSettlementAccount().getAccountNumber(),
                ledger.getSettlementAccount().getParticipant().getInstitutionId().value(),
                ledger.getEntryType().name(),
                ledger.amount().amount(),
                ledger.balanceAfter().amount(),
                timestamp(ledger.getCreatedAt())
        );
    }

    private static OffsetDateTime timestamp(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
