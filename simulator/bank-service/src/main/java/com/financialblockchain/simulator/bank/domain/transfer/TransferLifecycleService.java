package com.financialblockchain.simulator.bank.domain.transfer;

import com.financialblockchain.simulator.bank.support.DuplicateTransactionException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferLifecycleService {

    private final OutgoingTransferRepository repository;

    public TransferLifecycleService(OutgoingTransferRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public OutgoingTransfer create(
            String transactionId,
            String sourceAccountNumber,
            String destinationBankId,
            String destinationAccountNumber,
            BigDecimal amount
    ) {
        if (repository.existsByTransactionId(transactionId)) {
            throw new DuplicateTransactionException(transactionId);
        }
        return repository.save(OutgoingTransfer.start(
                transactionId,
                sourceAccountNumber,
                destinationBankId,
                destinationAccountNumber,
                amount
        ));
    }

    @Transactional
    public OutgoingTransfer markDebitPosted(String transactionId) {
        OutgoingTransfer transfer = getRequired(transactionId);
        transfer.markDebitPosted();
        return transfer;
    }

    @Transactional
    public OutgoingTransfer markCompleted(String transactionId) {
        OutgoingTransfer transfer = getRequired(transactionId);
        transfer.markCompleted();
        return transfer;
    }

    @Transactional
    public OutgoingTransfer markDebitFailed(String transactionId, String reason) {
        OutgoingTransfer transfer = getRequired(transactionId);
        transfer.markDebitFailed(shortReason(reason));
        return transfer;
    }

    @Transactional
    public OutgoingTransfer markCreditFailed(String transactionId, String reason) {
        OutgoingTransfer transfer = getRequired(transactionId);
        transfer.markCreditFailed(shortReason(reason));
        return transfer;
    }

    @Transactional(readOnly = true)
    public OutgoingTransfer get(String transactionId) {
        return getRequired(transactionId);
    }

    private OutgoingTransfer getRequired(String transactionId) {
        return repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + transactionId));
    }

    private static String shortReason(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.substring(0, Math.min(reason.length(), 500));
    }
}
