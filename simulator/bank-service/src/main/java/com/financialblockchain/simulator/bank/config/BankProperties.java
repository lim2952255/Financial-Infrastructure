package com.financialblockchain.simulator.bank.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 현재 은행의 식별자와 통신 대상인 상대 은행 정보를 환경설정에서 읽는다. */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(
        String id,
        String counterpartId,
        URI counterpartBaseUrl
) {
}
