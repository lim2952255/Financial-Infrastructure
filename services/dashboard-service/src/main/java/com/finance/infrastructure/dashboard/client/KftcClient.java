package com.finance.infrastructure.dashboard.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
// KFTC의 청산·결제 실행 API를 호출한다.
// 원래 KFTC의 청산 및 결제요청은 일정 주기마다 실행되는데 테스트상황에서는 직접 실행해볼 수 있도록 설정한다.
public class KftcClient extends UpstreamClientSupport {

    private final RestClient restClient;

    public KftcClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public JsonNode settle(String kftcBaseUrl, String cycleId) {
        return call("KFTC", () -> restClient.post()
                .uri(kftcBaseUrl + "/api/clearing-cycles/{cycleId}/settlement", cycleId)
                .retrieve()
                .body(JsonNode.class));
    }
}
