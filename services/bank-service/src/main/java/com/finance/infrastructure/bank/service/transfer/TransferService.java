package com.finance.infrastructure.bank.service.transfer;

import com.finance.infrastructure.bank.config.BankProperties;
import com.finance.infrastructure.bank.service.transfer.interbank.InterbankTransferService;
import com.finance.infrastructure.bank.service.transfer.internal.InternalTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// 실제 사용자 계좌 간 이체를 처리하는 서비스
@Service
@RequiredArgsConstructor
public class TransferService {

    private final BankProperties bankProperties; // 현재 실행 주체의 기관정보
    private final InternalTransferService internalTransferService; // 은행 내 거래
    private final InterbankTransferService interbankTransferService; // 은행 간 송금

    public TransferResult transfer(Long customerId, TransferCommand command) {
        // 수신기관이 다르면 KFTC를 거치는 은행 간 이체로 처리한다.
        if (!bankProperties.currentInstitutionId().equals(command.receiverInstitutionId())) {
            return interbankTransferService.transfer(customerId, command);
        }
        // 송신자 계좌의 은행과 수신자 계좌의 은행이 같은 경우, 즉 internal Transfer인 경우를 처리한다.
        return internalTransferService.transfer(customerId, command);
    }
}
