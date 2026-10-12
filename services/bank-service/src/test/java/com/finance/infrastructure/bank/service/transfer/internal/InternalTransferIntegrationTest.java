package com.finance.infrastructure.bank.service.transfer.internal;

import com.finance.infrastructure.bank.domain.account.DepositAccount;
import com.finance.infrastructure.bank.domain.customer.Customer;
import com.finance.infrastructure.bank.domain.ledger.BalanceChangeType;
import com.finance.infrastructure.bank.domain.ledger.Ledger;
import com.finance.infrastructure.bank.domain.transaction.TransactionStatus;
import com.finance.infrastructure.bank.repository.BankTransactionRepository;
import com.finance.infrastructure.bank.repository.CustomerRepository;
import com.finance.infrastructure.bank.repository.DepositAccountRepository;
import com.finance.infrastructure.bank.repository.LedgerRepository;
import com.finance.infrastructure.bank.service.transfer.TransferCommand;
import com.finance.infrastructure.bank.service.transfer.TransferResult;
import com.finance.infrastructure.bank.service.transfer.TransferService;
import com.finance.infrastructure.bank.support.MySqlIntegrationTestSupport;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc // 가상의 HTTP통신을 지원하는 MockMVC기능 설정
@SpringBootTest // 스프링부트의 전체 Application Context를 띄우는 통합테스트 설정
class InternalTransferIntegrationTest extends MySqlIntegrationTestSupport {

    @Autowired
    private TransferService transferService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DepositAccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository transactionRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    // HTTP 통신환경을 시뮬레이션할 수 있는 MockMvc 추가
    @Autowired
    private MockMvc mockMvc;

    private Customer senderCustomer;
    private Customer receiverCustomer;
    private DepositAccount senderAccount;
    private DepositAccount receiverAccount;

    // 매 테스트가 실행되기 이전에 호출된다. 이는 이전 테스트에서 사용됐던 데이터들을 초기화하고, 다음 테스트를 위한 테스트데이터들을 설정하는 역할을 수행한다.
    @BeforeEach
    void setUp() {
        ledgerRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();

        // 송신자와 수신자, 송신자 계정과 수신자 계정을 추가한다.
        senderCustomer = customerRepository.save(new Customer("sender"));
        receiverCustomer = customerRepository.save(new Customer("receiver"));

        senderAccount = new DepositAccount("100-001", senderCustomer);
        senderAccount.increase(Money.of(new BigDecimal("100000")));
        senderAccount = accountRepository.save(senderAccount);
        receiverAccount = accountRepository.save(
                new DepositAccount("100-002", receiverCustomer)
        );
    }

    // 동일 은행 내 거래에서 송수신시에는 하나의 Transaction을 저장하고, 송신자 Ledger와 수신자 Ledger를 기록해야 한다.
    @Test
    void transfersMoneyAndRecordsTransactionAndTwoLedgerEntries() {
        // 송신자가 수신자에게 30000원 이체
        TransferResult result = transferService.transfer(senderCustomer.getId(), command(
                "transfer-001",
                new BigDecimal("30000")
        ));

        assertThat(result.status()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.sender().institutionId()).isEqualTo(InstitutionId.of("BANK_A"));
        assertThat(accountRepository.findById(senderAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("70000")));
        assertThat(accountRepository.findById(receiverAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("30000")));

        // 송신자 ledger와 수신자 ledger가 정상적으로 저장되었는지를 검사한다.
        List<Ledger> ledgers = ledgerRepository.findAllByBankTransactionIdOrderByIdAsc(
                result.transactionId()
        );
        assertThat(ledgers).hasSize(2);
        assertThat(ledgers).extracting(Ledger::getChangeType)
                .containsExactly(BalanceChangeType.DECREASE, BalanceChangeType.INCREASE);
        assertThat(ledgers).extracting(Ledger::balanceAfter)
                .containsExactly(
                        Money.of(new BigDecimal("70000")),
                        Money.of(new BigDecimal("30000"))
                );
    }

    // 같은 은행내부의 거래요청 정보는 허용한다.
    @Test
    void acceptsAnInternalTransferAtTheUnversionedRestEndpoint() throws Exception {
        // mockMvc를 통해 가상으로 HTTP 메세지를 전송하는 상황을 시뮬레이션 할 수 있다.
        mockMvc.perform(post("/api/customers/{customerId}/transfers", senderCustomer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "rest-transfer-001",
                                  "senderAccountNumber": "100-001",
                                  "receiverInstitutionId": "BANK_A",
                                  "receiverAccountNumber": "100-002",
                                  "amount": 30000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("rest-transfer-001"))
                .andExpect(jsonPath("$.type").value("INTERNAL_TRANSFER"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.sender.institutionId").value("BANK_A"))
                .andExpect(jsonPath("$.receiver.accountNumber").value("100-002"))
                .andExpect(jsonPath("$.amount").value(30000));

        // 같은 은행 내의 거래는 이체에 성공하여 금액이 정상적으로 update되어야 한다.
        assertThat(accountRepository.findById(senderAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("70000")));
        assertThat(accountRepository.findById(receiverAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("30000")));
    }

    // 잘못된 금액 이체요청은 거부해야 한다.
    @Test
    void rejectsARequestWhenTheAmountIsNotPositive() throws Exception {
        mockMvc.perform(post("/api/customers/{customerId}/transfers", senderCustomer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "invalid-amount-001",
                                  "senderAccountNumber": "100-001",
                                  "receiverInstitutionId": "BANK_A",
                                  "receiverAccountNumber": "100-002",
                                  "amount": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(transactionRepository.count()).isZero();
        assertThat(ledgerRepository.count()).isZero();
    }

    // 원화단위 거래만 허용하므로 소수단위 거래요청정보는 거부해야한다.
    @Test
    void rejectsARequestWhenTheAmountContainsDecimalPlaces() throws Exception {
        mockMvc.perform(post("/api/customers/{customerId}/transfers", senderCustomer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "decimal-amount-001",
                                  "senderAccountNumber": "100-001",
                                  "receiverInstitutionId": "BANK_A",
                                  "receiverAccountNumber": "100-002",
                                  "amount": 1000.5
                                }
                                """))
                .andExpect(status().isBadRequest());

        assertThat(transactionRepository.count()).isZero();
        assertThat(ledgerRepository.count()).isZero();
    }

    // 같은 이체요청 정보는 두번 처리하지 않도록 하여 멱등성을 보장해야 한다.
    @Test
    void sameRequestDoesNotTransferMoneyTwice() {
        TransferCommand command = command("transfer-002", new BigDecimal("30000"));

        // 똑같은 거래 요청정보를 두번 전송한다.
        TransferResult first = transferService.transfer(senderCustomer.getId(), command);
        TransferResult second = transferService.transfer(senderCustomer.getId(), command);

        assertThat(second.transactionId()).isEqualTo(first.transactionId());
        // 첫번째 거래요청정보만 처리하고, 두번째 거래 요청은 수행하지 않음으로서 멱등성을 보장해야 한다.
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(ledgerRepository.count()).isEqualTo(2);
        assertThat(accountRepository.findById(senderAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("70000")));
        assertThat(accountRepository.findById(receiverAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("30000")));
    }

    // 송신자가 보유한 예금보다 더 많은 금액을 송신하려고 시도하면 거부해야 한다.
    @Test
    void insufficientBalanceRollsBackTheWholeTransfer() {
        assertThatThrownBy(() -> transferService.transfer(senderCustomer.getId(), command(
                "transfer-003",
                new BigDecimal("100001")
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("insufficient account balance");

        assertThat(transactionRepository.count()).isZero();
        assertThat(ledgerRepository.count()).isZero();
        assertThat(accountRepository.findById(senderAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("100000")));
        assertThat(accountRepository.findById(receiverAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.zero());
    }

    // 이체를 하기위해 송신자 계좌와 수신자 계좌에 lock을 획득할때, Account_Id가 작은 계좌부터 lock을 획득함으로서 deadlock을 방지한다.
    // 해당 테스트에서는 A->B 이체와 B->A이체를 동시에 실행해도 항상 Account_id가 작은 계좌부터 lock을 획득함으로서 deadlock을 방지하는지를 테스트한다.
    @Test
    void completesOpposingConcurrentTransfersWithoutDeadlock() throws Exception {
        receiverAccount.increase(Money.of(new BigDecimal("100000")));
        receiverAccount = accountRepository.save(receiverAccount);

        // A->B 이체용 스레드와 B->A 이체용 스레드를 생성한다.
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2); // 두 스레드가 모두 준비되었는지 확인하는 용도 -> count가 0이 되어야 다음 코드를 실행한다.
        CountDownLatch start = new CountDownLatch(1); // 두 스레드를 동시에 시작하게 만드는 용도 -> count가 0이 되어야 다음 코드를 실행한다.

        try {
            // A->B
            Future<TransferResult> senderToReceiver = executor.submit(() -> {
                ready.countDown(); // ready를 1만큼 감소시킨다.
                start.await(); // start에서 대기
                return transferService.transfer(senderCustomer.getId(), command(
                        "concurrent-transfer-001",
                        senderAccount,
                        receiverAccount,
                        new BigDecimal("30000")
                ));
            });
            // B->A
            Future<TransferResult> receiverToSender = executor.submit(() -> {
                ready.countDown(); // ready를 1만큼 감소시킨다.
                start.await(); // start에서 대기
                return transferService.transfer(receiverCustomer.getId(), command(
                        "concurrent-transfer-002",
                        receiverAccount,
                        senderAccount,
                        new BigDecimal("30000")
                ));
            });

            // 5초동안 대기
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown(); // start를 1만큼 감소시켜 대기중이던 두 스레드가 동시에 실행하도록 만든다.

            // 두 거래요청정보가 데드락없이 성공적으로 수행되는지를 검사한다.
            assertThat(senderToReceiver.get(10, TimeUnit.SECONDS).status())
                    .isEqualTo(TransactionStatus.COMPLETED);
            assertThat(receiverToSender.get(10, TimeUnit.SECONDS).status())
                    .isEqualTo(TransactionStatus.COMPLETED);
        } finally {
            executor.shutdownNow();
        }

        // A->B 이체와 B->A 이체를 동시에 실행해도 데드락없이 성공적으로 실행되는지를 검사한다.
        assertThat(accountRepository.findById(senderAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("100000")));
        assertThat(accountRepository.findById(receiverAccount.getId()).orElseThrow().balance())
                .isEqualTo(Money.of(new BigDecimal("100000")));
        assertThat(transactionRepository.count()).isEqualTo(2);
        assertThat(ledgerRepository.count()).isEqualTo(4);
    }

    // 이체 요청정보를 담은 command를 생성한다. (송신자 -> 수신자)
    private TransferCommand command(String requestId, BigDecimal amount) {
        return command(requestId, senderAccount, receiverAccount, amount);
    }

    private TransferCommand command(
            String requestId,
            DepositAccount sender,
            DepositAccount receiver,
            BigDecimal amount
    ) {
        return new TransferCommand(
                requestId,
                sender.getAccountNumber(),
                // 테스트환경의 기관설정정보: BANK_A
                InstitutionId.of("BANK_A"), // 수신자 은행 기관을 송신자 은행 기관과 맞춤으로서 같은 은행 내 이체 상황을 테스트한다.
                receiver.getAccountNumber(),
                Money.of(amount)
        );
    }
}
