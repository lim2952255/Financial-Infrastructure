package com.finance.infrastructure.kftc.controller.clearing;

import com.finance.infrastructure.kftc.service.clearing.ClearingResult;
import com.finance.infrastructure.kftc.service.clearing.ClearingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 운영 스케줄러가 호출할 수 있는 청산주기 실행 API다.
// 해당 API를 통해 특정 청산주기의 청산을 수행하고,청산 결과를 BOK에 전달하고, BOK의 결제결과를 응답받는다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clearing-cycles")
public class ClearingController {

    // 특정 청산주기동안 모인 지급지시정보들을 모아서 청산하고, 청산결과를 BOK에 전달하고, BOK의 결제결과를 받는 서비스
    private final ClearingService clearingService;

    @PostMapping("/{cycleId}/settlement")
    public ResponseEntity<ClearingResult> clearAndSettle(@PathVariable String cycleId) {
        return ResponseEntity.ok(clearingService.clearAndSettle(cycleId));
    }
}
