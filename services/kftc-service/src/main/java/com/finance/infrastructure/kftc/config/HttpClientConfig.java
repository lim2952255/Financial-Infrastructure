package com.finance.infrastructure.kftc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// 은행과 BOK를 호출하는 클라이언트들이 공유할 RestClient 생성기를 스프링 빈으로 등록한다.
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
