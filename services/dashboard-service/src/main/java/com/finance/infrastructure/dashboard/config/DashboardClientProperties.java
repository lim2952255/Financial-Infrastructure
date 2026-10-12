package com.finance.infrastructure.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// application.properties에서 finance.clients.* 관련 설정변수들을 읽어 record에 바인딩한다.
@ConfigurationProperties(prefix = "finance.clients")
public record DashboardClientProperties(
        String bankABaseUrl,
        String bankBBaseUrl,
        String bankCBaseUrl,
        String kftcBaseUrl,
        String bokBaseUrl
) {
}
