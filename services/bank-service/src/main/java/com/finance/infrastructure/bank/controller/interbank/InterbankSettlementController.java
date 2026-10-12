package com.finance.infrastructure.bank.controller.interbank;

import com.finance.infrastructure.bank.service.transfer.interbank.InterbankSettlementCommand;
import com.finance.infrastructure.bank.service.transfer.interbank.InterbankSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// KFTC가 청산주기 단위로 송신은행과 수신은행에 BOK 최종 결제 완료를 통보한다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/interbank-settlements")
public class InterbankSettlementController {

    // KFTC가 보낸 BOK 최종결제 완료 정보를 바탕으로 Transaction의 상태를 update하는 서비스
    private final InterbankSettlementService settlementService;

    @PostMapping
    public ResponseEntity<Void> confirm(@RequestBody InterbankSettlementCommand command) {
        settlementService.confirm(command);
        return ResponseEntity.noContent().build();
    }
}
