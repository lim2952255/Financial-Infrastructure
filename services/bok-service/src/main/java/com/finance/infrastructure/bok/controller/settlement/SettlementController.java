package com.finance.infrastructure.bok.controller.settlement;

import com.finance.infrastructure.bok.service.settlement.SettlementCommand;
import com.finance.infrastructure.bok.service.settlement.SettlementResult;
import com.finance.infrastructure.bok.service.settlement.SettlementService;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

// KFTC의 청산 결과를 접수하여 BOK 당좌예금 결제를 실행하는 API다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlement-batches")
public class SettlementController {

    // KFTC로부터 청산 결과를 접수하고, 이를 기반으로 각 기관별 당좌예금 계좌에서 당좌예금 결제를 수행하는 서비스
    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<SettlementResult> settle(
            @RequestBody SettlementRequest request
    ) {
        // KFTC로부터 받은 결제 요청 정보를 기반으로 결제를 수행하고, 결제 결과를 리턴받는다.
        SettlementResult result = settlementService.settle(new SettlementCommand(
                request.batchId(),
                request.clearingCycleId(),
                // 순포지션금액을 BigDecimal -> Money로 변환한다.
                request.positions().stream()
                        .map(position -> new SettlementCommand.PositionCommand(
                                InstitutionId.of(position.institutionId()),
                                Money.of(position.netAmount())
                        ))
                        .toList()
        ));
        return ResponseEntity.ok(result);
    }

    // KFTC로부터 받은 청산결과 및 결제요청 정보를 담은 record
    public record SettlementRequest(
            String batchId,
            String clearingCycleId,
            List<PositionRequest> positions
    ) {
    }

    // 각 금융기관별 순포지션을 저장한 레코드
    public record PositionRequest(String institutionId, BigDecimal netAmount) {
    }

}
