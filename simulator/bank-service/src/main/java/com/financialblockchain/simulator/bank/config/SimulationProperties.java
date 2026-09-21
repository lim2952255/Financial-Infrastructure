package com.financialblockchain.simulator.bank.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 시작 시 생성할 데모 계좌 정보를 환경설정에서 읽는다. */
@ConfigurationProperties(prefix = "simulation.seed")
public record SimulationProperties(
        boolean enabled,
        String accountNumber,
        String accountHolder,
        BigDecimal openingBalance
) {
}
