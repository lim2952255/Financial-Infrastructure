package com.finance.infrastructure.dashboard.controller;

import com.finance.infrastructure.dashboard.service.DashboardService;
import com.finance.infrastructure.dashboard.service.DashboardSnapshot;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/dashboard")
// 프론트엔드에 각 기관으로부터 받은 통합 데이터, 이체, 청산·결제 진입점을 제공한다.
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // 각 기관들의 모니터링 정보를 받아서 응답한다.
    @GetMapping("/snapshot")
    public ResponseEntity<DashboardSnapshot> snapshot() {
        return ResponseEntity.ok(dashboardService.snapshot());
    }

    // 송신은행에게 이체 요청을 전송한다.
    @PostMapping("/transfers")
    public ResponseEntity<JsonNode> transfer(@Valid @RequestBody DashboardTransferRequest request) {
        return ResponseEntity.ok(dashboardService.transfer(request));
    }

    // KFTC에게 청산 및 결제 요청을 전송한다.
    @PostMapping("/clearing-cycles/{cycleId}/settlement")
    public ResponseEntity<JsonNode> clearAndSettle(@PathVariable String cycleId) {
        return ResponseEntity.ok(dashboardService.clearAndSettle(cycleId));
    }
}
