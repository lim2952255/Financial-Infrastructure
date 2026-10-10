package com.finance.infrastructure.bank.service.transaction;

import com.finance.infrastructure.bank.config.BankProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

// 실제 사용자 계좌 간 이체를 처리하는 서비스
@Service
@RequiredArgsConstructor
public class TransferService {

    private final BankProperties bankProperties; // 현재 실행 주체의 기관정보
    private final InternalTransferService internalTransferService; // 은행 내 거래

    public TransferResult transfer(Long customerId, TransferCommand command) {
        // 만약 송신자 계좌의 은행과 수신자 계좌의 은행이 다른 경우는 interbank transfer이므로 아직 구현 x
        if (!bankProperties.currentInstitutionId().equals(command.receiverInstitutionId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_IMPLEMENTED,
                    "interbank transfer processing is not implemented yet"
            );
        }
        // 송신자 계좌의 은행과 수신자 계좌의 은행이 같은 경우, 즉 internal Transfer인 경우를 처리한다.
        return internalTransferService.transfer(customerId, command);
    }
}
