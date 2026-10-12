package com.finance.infrastructure.bok.controller.monitoring;

import com.finance.infrastructure.bok.service.monitoring.BokMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/monitoring")
// BOK의 참가은행, 참가은행들의 당좌예금 계좌, 결재 배치, 원장기록등을 record에 담아 대시보드에 응답한다.
public class BokMonitoringController {

    // BOK의 참가은행, 참가은행들의 당좌예금 계좌, 결재 배치, 원장기록등을 record로 생성하는 서비스
    private final BokMonitoringService monitoringService;

    @GetMapping
    public BokMonitoringResponse getMonitoringSnapshot() {
        return monitoringService.getMonitoringSnapshot();
    }
}
