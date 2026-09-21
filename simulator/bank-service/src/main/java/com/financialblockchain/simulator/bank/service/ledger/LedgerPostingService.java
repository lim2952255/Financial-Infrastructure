package com.financialblockchain.simulator.bank.service.ledger;

import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.domain.account.Account;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerEntry;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerEntryType;
import com.financialblockchain.simulator.bank.exception.AccountNotFoundException;
import com.financialblockchain.simulator.bank.exception.DuplicateTransactionException;
import com.financialblockchain.simulator.bank.repository.account.AccountRepository;
import com.financialblockchain.simulator.bank.repository.ledger.LedgerEntryRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한 은행의 로컬 DB 안에서 잔액 변경과 원장 생성을 하나의 트랜잭션으로 처리한다.
 * 이 트랜잭션은 상대 은행 DB까지 확장되지 않는다는 점이 시뮬레이션의 핵심이다.
 */
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
        // 동일 거래의 출금 원장이 두 번 기록되는 것을 먼저 차단한다.
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
        // 상대 은행의 재시도나 중복 전달로 같은 입금이 두 번 반영되는 것을 차단한다.
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
