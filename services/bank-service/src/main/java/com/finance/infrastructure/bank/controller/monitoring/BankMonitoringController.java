package com.finance.infrastructure.bank.controller.monitoring;

import com.finance.infrastructure.bank.service.monitoring.BankMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/monitoring")
// 대시보드가 은행의 계좌·거래·원장 상태를 읽을 수 있는 조회 전용 API다.
public class BankMonitoringController {

    // 대시보드에 전달할 은행의 계좌·거래·원장 상태 응답 데이터를 생성하는 서비스
    private final BankMonitoringService monitoringService;

    @GetMapping
    public BankMonitoringResponse getMonitoringSnapshot() {
        return monitoringService.getMonitoringSnapshot();
    }
}
