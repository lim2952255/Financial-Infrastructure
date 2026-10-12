package com.finance.infrastructure.kftc.controller.monitoring;

import com.finance.infrastructure.kftc.service.monitoring.KftcMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/monitoring")
// KFTC의 지급지시, 청산주기와 결제 제출 상태 정보를 대시보드에 응답한다.
public class KftcMonitoringController {

    // KFTC 참가자·지급지시·청산·결제 제출 데이터를 record에 담아 대시보드에 응답하는 서비스
    private final KftcMonitoringService monitoringService;

    @GetMapping
    public KftcMonitoringResponse getMonitoringSnapshot() {
        return monitoringService.getMonitoringSnapshot();
    }
}
