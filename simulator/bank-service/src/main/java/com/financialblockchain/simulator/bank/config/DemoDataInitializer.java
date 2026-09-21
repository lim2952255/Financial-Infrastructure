package com.financialblockchain.simulator.bank.config;

import com.financialblockchain.simulator.bank.service.account.AccountService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 시뮬레이션 옵션이 켜진 경우 은행별 기본 계좌를 한 번만 생성한다. */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final SimulationProperties simulationProperties;
    private final AccountService accountService;

    public DemoDataInitializer(
            SimulationProperties simulationProperties,
            AccountService accountService
    ) {
        this.simulationProperties = simulationProperties;
        this.accountService = accountService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!simulationProperties.enabled()) {
            return;
        }
        accountService.createIfMissing(
                simulationProperties.accountNumber(),
                simulationProperties.accountHolder(),
                simulationProperties.openingBalance()
        );
    }
}
