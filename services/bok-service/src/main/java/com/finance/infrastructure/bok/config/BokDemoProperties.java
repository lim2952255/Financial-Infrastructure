package com.finance.infrastructure.bok.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.properties에서 demo.*에 해당하는 설정변수들을 해당 레코드에 바인딩한다.
@ConfigurationProperties(prefix = "demo")
// BOK의 데모 참가자 및 참가 계좌를 데이터베이스에 추가할지 여부를 binding한다.
public record BokDemoProperties(boolean seedEnabled) {
}
