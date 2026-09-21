package com.financialblockchain.simulator.bank.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** 상대 은행 호출에 사용할 HTTP 클라이언트와 장애 감지 제한 시간을 설정한다. */
@Configuration
public class HttpClientConfig {

    @Bean
    RestClient counterpartRestClient(RestClient.Builder builder, BankProperties bankProperties) {
        // 상대 은행이 중단되더라도 송금 요청이 무기한 대기하지 않게 짧은 제한 시간을 둔다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        return builder
                .baseUrl(bankProperties.counterpartBaseUrl().toString())
                .requestFactory(requestFactory)
                .build();
    }
}
