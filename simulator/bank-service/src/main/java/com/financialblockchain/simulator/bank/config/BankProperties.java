package com.financialblockchain.simulator.bank.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank")
public record BankProperties(
        String id,
        String counterpartId,
        URI counterpartBaseUrl
) {
}
