package com.financialblockchain.simulator.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 동일한 실행 파일을 환경변수에 따라 Bank A 또는 Bank B로 구동하는 진입점이다. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankServiceApplication.class, args);
    }
}
