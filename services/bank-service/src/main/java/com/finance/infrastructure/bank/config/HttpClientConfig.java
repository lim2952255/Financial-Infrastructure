package com.finance.infrastructure.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// 기관 간 동기 HTTP 호출에서 공통으로 사용할 RestClient.Builder를 스프링 빈으로 등록한다.
@Configuration
public class HttpClientConfig {

    //RestClient.Builder는 HTTP 요청을 보내는 RestClient 객체를 설정하고 생성하는 도구이다.
    // RestClient는 실제 HTTP 요청을 담당하는 클라이언트이고, RestClient.Builder는 주소, 헤더 등을 설정해서 RestClient를 생성한다.
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
