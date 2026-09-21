package com.financialblockchain.simulator.bank.domain.ledger;

import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.domain.account.Account;
import com.financialblockchain.simulator.bank.domain.account.AccountRepository;
import com.financialblockchain.simulator.bank.support.AccountNotFoundException;
import com.financialblockchain.simulator.bank.support.DuplicateTransactionException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerPostingService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BankProperties bankProperties;

    public LedgerPostingService(
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository,
            BankProperties bankProperties
    ) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.bankProperties = bankProperties;
    }

    @Transactional
    public LedgerEntry postDebit(
            String transactionId,
            String accountNumber,
            BigDecimal amount,
            String counterpartyBankId,
            String counterpartyAccountNumber
    ) {
        rejectDuplicate(transactionId, LedgerEntryType.DEBIT);
        Account account = lockedAccount(accountNumber);
        BigDecimal balanceBefore = account.getBalance();
        account.withdraw(amount);
        return ledgerEntryRepository.save(LedgerEntry.posted(
                transactionId,
                bankProperties.id(),
                accountNumber,
                LedgerEntryType.DEBIT,
                amount,
                balanceBefore,
                account.getBalance(),
                counterpartyBankId,
                counterpartyAccountNumber
        ));
    }

    @Transactional
    public LedgerEntry postCredit(
            String transactionId,
            String accountNumber,
            BigDecimal amount,
            String counterpartyBankId,
            String counterpartyAccountNumber
    ) {
        rejectDuplicate(transactionId, LedgerEntryType.CREDIT);
        Account account = lockedAccount(accountNumber);
        BigDecimal balanceBefore = account.getBalance();
        account.deposit(amount);
        return ledgerEntryRepository.save(LedgerEntry.posted(
                transactionId,
                bankProperties.id(),
                accountNumber,
                LedgerEntryType.CREDIT,
                amount,
                balanceBefore,
                account.getBalance(),
                counterpartyBankId,
                counterpartyAccountNumber
        ));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> findByTransactionId(String transactionId) {
        return ledgerEntryRepository.findByTransactionIdOrderById(transactionId);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> findAll() {
        return ledgerEntryRepository.findAllByOrderById();
    }

    private Account lockedAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void rejectDuplicate(String transactionId, LedgerEntryType type) {
        if (ledgerEntryRepository.existsByTransactionIdAndType(transactionId, type)) {
            throw new DuplicateTransactionException(transactionId);
        }
    }
}
