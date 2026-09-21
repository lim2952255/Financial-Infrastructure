package com.financialblockchain.simulator.bank.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "simulation.seed")
public record SimulationProperties(
        boolean enabled,
        String accountNumber,
        String accountHolder,
        BigDecimal openingBalance
) {
}
