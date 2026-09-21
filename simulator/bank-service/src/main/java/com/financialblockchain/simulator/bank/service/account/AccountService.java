package com.financialblockchain.simulator.bank.service.account;

import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.domain.account.Account;
import com.financialblockchain.simulator.bank.exception.AccountNotFoundException;
import com.financialblockchain.simulator.bank.exception.DuplicateAccountException;
import com.financialblockchain.simulator.bank.repository.account.AccountRepository;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 계좌 생성과 조회를 담당하며 현재 은행 ID를 새 계좌에 자동으로 부여한다. */
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
        // Docker로 반복 실행할 때 데모 계좌가 중복 생성되지 않도록 한다.
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
