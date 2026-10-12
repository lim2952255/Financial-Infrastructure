package com.finance.infrastructure.bank;

import com.finance.infrastructure.bank.config.BankProperties;
import com.finance.infrastructure.bank.config.BankDemoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({BankProperties.class, BankDemoProperties.class})
public class BankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankServiceApplication.class, args);
    }
}
