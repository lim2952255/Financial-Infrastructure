package com.finance.infrastructure.bank.config;

import com.finance.infrastructure.bank.domain.account.DepositAccount;
import com.finance.infrastructure.bank.domain.customer.Customer;
import com.finance.infrastructure.bank.repository.CustomerRepository;
import com.finance.infrastructure.bank.repository.DepositAccountRepository;
import com.finance.infrastructure.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// ApplicationRunner를 구현해서 run메서드를 오버라이딩하면, Spring Boot가 실행과정에서 ApplicationRunner 타입의 빈을 찾아 run()을 한 번 호출한다.
// 즉 Spring Boot가 실행될때 데모데이터를 저장하는 역할을 수행한다.
@Component
@RequiredArgsConstructor
// 데모 환경에서 각 은행의 고객 두 명과 계좌 두 개를 계좌 단위로 멱등 생성한다.
public class BankDemoDataSeeder implements ApplicationRunner {

    private final BankDemoProperties properties; // 데모데이터로 넣을 설정들을 저장하고 있는 record
    private final CustomerRepository customerRepository;
    private final DepositAccountRepository accountRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // 만약 seedEnabled가 false라면 데이터를 삽입하지 않고 리턴한다.
        if (!properties.seedEnabled()) {
            return;
        }

        // 데모 계좌 생성
        List<DemoAccount> demoAccounts = List.of(
                new DemoAccount(
                        properties.customerName(),
                        properties.accountNumber(),
                        properties.accountBalance()
                ),
                new DemoAccount(
                        properties.secondaryCustomerName(),
                        properties.secondaryAccountNumber(),
                        properties.secondaryAccountBalance()
                )
        );
        demoAccounts.forEach(this::seed);
    }

    // 이미 repository에 해당 데모 데이터들이 저장되어 있지 않으면 데모 데이터를 저장한다. 이미 저장되어 있으면 중복으로 저장하지 않는다.
    private void seed(DemoAccount demo) {
        if (accountRepository.existsByAccountNumber(demo.accountNumber())) {
            return;
        }
        Customer customer = customerRepository
                .findFirstByNameOrderByIdAsc(demo.customerName())
                .orElseGet(() -> customerRepository.save(new Customer(demo.customerName())));
        DepositAccount account = new DepositAccount(demo.accountNumber(), customer);
        if (demo.accountBalance().signum() > 0) {
            account.increase(Money.of(demo.accountBalance()));
        }
        accountRepository.save(account);
    }

    private record DemoAccount(
            String customerName,
            String accountNumber,
            BigDecimal accountBalance
    ) {
    }
}
