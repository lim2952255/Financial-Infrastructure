package com.finance.infrastructure.dashboard.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
// 각 기관의 조회 전용 모니터링 API를 동일한 방식으로 호출한다.
public class MonitoringClient extends UpstreamClientSupport {

    private final RestClient restClient;

    public MonitoringClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public JsonNode getMonitoring(String institution, String baseUrl) {
        return call(institution, () -> restClient.get()
                .uri(baseUrl + "/api/monitoring")
                .retrieve()
                .body(JsonNode.class));
    }
}
