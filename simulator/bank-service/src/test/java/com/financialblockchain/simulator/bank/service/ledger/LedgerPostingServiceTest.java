package com.financialblockchain.simulator.bank.service.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.domain.account.Account;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerEntry;
import com.financialblockchain.simulator.bank.domain.ledger.LedgerEntryType;
import com.financialblockchain.simulator.bank.repository.account.AccountRepository;
import com.financialblockchain.simulator.bank.repository.ledger.LedgerEntryRepository;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 계좌 잔액 변경과 원장 생성이 한 서비스 호출에서 함께 수행되는지 검증한다. */
@ExtendWith(MockitoExtension.class)
class LedgerPostingServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void debitUpdatesAccountAndCreatesImmutableLedgerEntry() {
        Account account = Account.open("BANK_A", "A-1001", "Alice", new BigDecimal("1000.00"));
        when(ledgerEntryRepository.existsByTransactionIdAndType("TX-001", LedgerEntryType.DEBIT))
                .thenReturn(false);
        when(accountRepository.findByAccountNumberForUpdate("A-1001"))
                .thenReturn(Optional.of(account));
        when(ledgerEntryRepository.save(any(LedgerEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LedgerPostingService service = new LedgerPostingService(
                accountRepository,
                ledgerEntryRepository,
                new BankProperties("BANK_A", "BANK_B", URI.create("http://bank-b:8080"))
        );

        LedgerEntry entry = service.postDebit(
                "TX-001",
                "A-1001",
                new BigDecimal("100.00"),
                "BANK_B",
                "B-2001"
        );

        assertThat(account.getBalance()).isEqualByComparingTo("900.00");
        assertThat(entry.getBalanceBefore()).isEqualByComparingTo("1000.00");
        assertThat(entry.getBalanceAfter()).isEqualByComparingTo("900.00");
        assertThat(entry.getType()).isEqualTo(LedgerEntryType.DEBIT);
    }
}
