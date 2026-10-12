package com.finance.infrastructure.bank.service.monitoring;

import com.finance.infrastructure.bank.config.BankProperties;
import com.finance.infrastructure.bank.controller.monitoring.BankMonitoringResponse;
import com.finance.infrastructure.bank.domain.account.DepositAccount;
import com.finance.infrastructure.bank.domain.ledger.Ledger;
import com.finance.infrastructure.bank.domain.transaction.BankTransaction;
import com.finance.infrastructure.bank.repository.BankTransactionRepository;
import com.finance.infrastructure.bank.repository.DepositAccountRepository;
import com.finance.infrastructure.bank.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
// 계좌, 거래, 원장상태를 대시보드에 전달해줄 응답 데이터를 생성하는 서비스
public class BankMonitoringService {

    private final BankProperties bankProperties;
    private final DepositAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public BankMonitoringResponse getMonitoringSnapshot() {
        return new BankMonitoringResponse(
                bankProperties.currentInstitutionId().value(), // 현재 기관정보
                accountRepository.findAllForMonitoring().stream().map(this::account).toList(), // 은행내의 모든 계좌 조회
                transactionRepository.findAllByOrderByIdAsc().stream().map(this::transaction).toList(), // 은행 내의 모든 트랜잭션 조회
                ledgerRepository.findAllForMonitoring().stream().map(this::ledgerEntry).toList() // 은행 내의 모든 원장 조회
        );
    }

    private BankMonitoringResponse.Account account(DepositAccount account) {
        return new BankMonitoringResponse.Account(
                account.getCustomer().getId(),
                account.getCustomer().getName(),
                account.getAccountNumber(),
                account.balance().amount()
        );
    }

    private BankMonitoringResponse.Transaction transaction(BankTransaction transaction) {
        return new BankMonitoringResponse.Transaction(
                transaction.getId(),
                transaction.getRequestId(),
                transaction.getPaymentInstructionId(),
                transaction.getType().name(),
                transaction.getStatus().name(),
                transaction.sender().institutionId().value(),
                transaction.sender().accountNumber(),
                transaction.receiver().institutionId().value(),
                transaction.receiver().accountNumber(),
                transaction.amount().amount(),
                transaction.getClearingCycleId(),
                transaction.getSettlementBatchId(),
                timestamp(transaction.getCreatedAt()),
                timestamp(transaction.getCompletedAt()),
                timestamp(transaction.getSettlementCompletedAt()),
                transaction.getFailureReason()
        );
    }

    private BankMonitoringResponse.LedgerEntry ledgerEntry(Ledger ledger) {
        BankTransaction transaction = ledger.getBankTransaction();
        return new BankMonitoringResponse.LedgerEntry(
                ledger.getId(),
                transaction.getId(),
                transaction.getRequestId(),
                transaction.getPaymentInstructionId(),
                ledger.getAccount().getAccountNumber(),
                ledger.getChangeType().name(),
                ledger.amount().amount(),
                ledger.balanceAfter().amount(),
                timestamp(ledger.getCreatedAt())
        );
    }

    private static OffsetDateTime timestamp(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
