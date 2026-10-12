package com.finance.infrastructure.kftc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.properties에서 demo.*로 설정된 설정변수들을 해당 record에 바인딩한다.
@ConfigurationProperties(prefix = "demo")
// KFTC 데모 참가자 데이터를 준비할지 여부를 binding한다.
public record KftcDemoProperties(boolean seedEnabled) {
}
