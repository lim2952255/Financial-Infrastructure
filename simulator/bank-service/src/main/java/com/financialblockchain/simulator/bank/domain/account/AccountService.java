package com.financialblockchain.simulator.bank.domain.account;

import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.support.AccountNotFoundException;
import com.financialblockchain.simulator.bank.support.DuplicateAccountException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final BankProperties bankProperties;

    public AccountService(AccountRepository accountRepository, BankProperties bankProperties) {
        this.accountRepository = accountRepository;
        this.bankProperties = bankProperties;
    }

    @Transactional
    public Account create(String accountNumber, String accountHolder, BigDecimal openingBalance) {
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new DuplicateAccountException(accountNumber);
        }
        return accountRepository.save(Account.open(
                bankProperties.id(),
                accountNumber,
                accountHolder,
                openingBalance
        ));
    }

    @Transactional(readOnly = true)
    public Account get(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Transactional
    public void createIfMissing(String accountNumber, String accountHolder, BigDecimal openingBalance) {
        if (!accountRepository.existsByAccountNumber(accountNumber)) {
            accountRepository.save(Account.open(
                    bankProperties.id(),
                    accountNumber,
                    accountHolder,
                    openingBalance
            ));
        }
    }
}
