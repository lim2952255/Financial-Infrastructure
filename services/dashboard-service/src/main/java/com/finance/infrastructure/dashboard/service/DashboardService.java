package com.finance.infrastructure.dashboard.service;

import com.finance.infrastructure.dashboard.client.BankClient;
import com.finance.infrastructure.dashboard.client.KftcClient;
import com.finance.infrastructure.dashboard.client.MonitoringClient;
import com.finance.infrastructure.dashboard.config.DashboardClientProperties;
import com.finance.infrastructure.dashboard.controller.DashboardTransferRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.databind.JsonNode;

import static com.finance.infrastructure.dashboard.service.DashboardSnapshot.*;

/**
 * 다섯 기관의 상태를 집계하고 이체·청산 요청을 담당 기관으로 라우팅한다.
 * 모니터링 조회는 기관별 실패를 격리하지만, 상태를 변경하는 이체·청산 요청의 실패는 성공으로 오인하지 않도록 호출자에게 그대로 전달한다.
 */
@Service
public class DashboardService {

    private final DashboardClientProperties properties;
    private final MonitoringClient monitoringClient;
    private final BankClient bankClient;
    private final KftcClient kftcClient;

    public DashboardService(
            DashboardClientProperties properties,
            MonitoringClient monitoringClient,
            BankClient bankClient,
            KftcClient kftcClient
    ) {
        this.properties = properties;
        this.monitoringClient = monitoringClient;
        this.bankClient = bankClient;
        this.kftcClient = kftcClient;
    }

    // 각 호출을 monitor()로 감싸므로 한 기관이 중단되어도 나머지 기관의 상태는 반환할 수 있다.
    public DashboardSnapshot snapshot() {
        Map<String, InstitutionSnapshot> institutions = new LinkedHashMap<>();
        institutions.put("BANK_A", monitor("BANK_A", properties.bankABaseUrl()));
        institutions.put("BANK_B", monitor("BANK_B", properties.bankBBaseUrl()));
        institutions.put("BANK_C", monitor("BANK_C", properties.bankCBaseUrl()));
        institutions.put("KFTC", monitor("KFTC", properties.kftcBaseUrl()));
        institutions.put("BOK", monitor("BOK", properties.bokBaseUrl()));
        return new DashboardSnapshot(Instant.now(), institutions);
    }

    // 송신은행에 이체를 요청한다. 실패하면 예외를 잡지 않고 프론트까지 전달한다.
    // 실제 입금이 끝나지 않은 요청을 Dashboard가 임의로 성공 처리해서는 안 되기 때문이다.
    public JsonNode transfer(DashboardTransferRequest request) {
        return bankClient.transfer(senderBankBaseUrl(request.senderInstitutionId()), request);
    }

    // KFTC에 청산·결제를 요청한다. 이 작업도 전체 결과가 중요하므로 실패를 격리하지 않는다.
    public JsonNode clearAndSettle(String cycleId) {
        return kftcClient.settle(properties.kftcBaseUrl(), cycleId);
    }

    // 모니터링 호출에서만 예외를 기관별 unavailable 결과로 바꾸어 전체 조회 실패를 방지한다.
    private InstitutionSnapshot monitor(String institution, String baseUrl) {
        try {
            return InstitutionSnapshot.available(
                    monitoringClient.getMonitoring(institution, baseUrl)
            );
        } catch (ResponseStatusException exception) {
            // UpstreamClientSupport가 변환한 오류를 이 기관의 상태에만 기록한다.
            return InstitutionSnapshot.unavailable(message(exception));
        }
    }

    // 송신은행의 URL정보 확인
    private String senderBankBaseUrl(String institutionId) {
        return switch (institutionId) {
            case "BANK_A" -> properties.bankABaseUrl();
            case "BANK_B" -> properties.bankBBaseUrl();
            case "BANK_C" -> properties.bankCBaseUrl();
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "unsupported sender institution: " + institutionId
            );
        };
    }

    private static String message(RuntimeException exception) {
        return exception.getMessage() == null ? "institution is unavailable" : exception.getMessage();
    }
}
