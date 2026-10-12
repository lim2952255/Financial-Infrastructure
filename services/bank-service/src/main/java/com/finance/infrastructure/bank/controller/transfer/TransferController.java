package com.finance.infrastructure.bank.controller.transfer;

import com.finance.infrastructure.bank.controller.transfer.dto.TransferRequest;
import com.finance.infrastructure.bank.controller.transfer.dto.TransferResponse;
import com.finance.infrastructure.bank.service.transfer.TransferCommand;
import com.finance.infrastructure.bank.service.transfer.TransferResult;
import com.finance.infrastructure.bank.service.transfer.TransferService;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/customers/{customerId}/transfers")
public class TransferController {

    private final TransferService transferService;

    // ResponseEntity는 응답 상태·헤더·본문을 직접 구성하는 객체이다.
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            @PathVariable @Positive Long customerId,
            @Valid @RequestBody TransferRequest request // @RequestBody를 통해 요청 본문의 JSON을 TransferRequest로 변환하고, 객체 내부의 검증을 수행한다.
    ) {
        TransferResult result = transferService.transfer(customerId, new TransferCommand(
                request.requestId(),
                request.senderAccountNumber(),
                InstitutionId.of(request.receiverInstitutionId()),
                request.receiverAccountNumber(),
                Money.of(request.amount())
        ));
        // 200 ok라는 응답 상태정보와 본문을 포함하여 응답을 생성한다.
        return ResponseEntity.ok(TransferResponse.from(result));
    }
}
