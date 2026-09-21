package com.financialblockchain.simulator.bank.service.transfer;

import com.financialblockchain.simulator.bank.dto.BankApiModels.IncomingCreditRequest;
import com.financialblockchain.simulator.bank.config.BankProperties;
import com.financialblockchain.simulator.bank.domain.transfer.OutgoingTransfer;
import com.financialblockchain.simulator.bank.service.ledger.LedgerPostingService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 출발 은행의 로컬 출금을 커밋한 뒤 상대 은행의 입금 API를 호출하는 송금 조정 서비스다.
 * 의도적으로 분산 트랜잭션을 사용하지 않아 상대 은행 실패 시 부분 성공을 관찰할 수 있다.
 */
@Service
public class OutgoingTransferService {

    private final LedgerPostingService ledgerPostingService;
    private final TransferLifecycleService lifecycleService;
    private final RestClient counterpartRestClient;
    private final BankProperties bankProperties;

    public OutgoingTransferService(
            LedgerPostingService ledgerPostingService,
            TransferLifecycleService lifecycleService,
            RestClient counterpartRestClient,
            BankProperties bankProperties
    ) {
        this.ledgerPostingService = ledgerPostingService;
        this.lifecycleService = lifecycleService;
        this.counterpartRestClient = counterpartRestClient;
        this.bankProperties = bankProperties;
    }

    public OutgoingTransfer transfer(
            String transactionId,
            String sourceAccountNumber,
            String destinationBankId,
            String destinationAccountNumber,
            BigDecimal amount,
            boolean simulateDestinationFailure
    ) {
        requireConfiguredCounterpart(destinationBankId);
        // 송금 지시 자체를 먼저 기록해 이후 실패 원인을 추적할 수 있게 한다.
        lifecycleService.create(
                transactionId,
                sourceAccountNumber,
                destinationBankId,
                destinationAccountNumber,
                amount
        );

        try {
            // 별도 서비스 빈의 @Transactional 메서드이므로 이 호출이 끝나면 출발 은행의 출금이 커밋된다.
            ledgerPostingService.postDebit(
                    transactionId,
                    sourceAccountNumber,
                    amount,
                    destinationBankId,
                    destinationAccountNumber
            );
            lifecycleService.markDebitPosted(transactionId);
        } catch (RuntimeException exception) {
            lifecycleService.markDebitFailed(transactionId, exception.getMessage());
            throw exception;
        }

        try {
            // 로컬 트랜잭션 밖의 HTTP 호출이므로 상대 은행 실패가 출발 은행의 출금을 롤백하지 못한다.
            counterpartRestClient.post()
                    .uri("/internal/transfers/credits")
                    .body(new IncomingCreditRequest(
                            transactionId,
                            destinationAccountNumber,
                            amount,
                            bankProperties.id(),
                            sourceAccountNumber,
                            simulateDestinationFailure
                    ))
                    .retrieve()
                    .toBodilessEntity();
            return lifecycleService.markCompleted(transactionId);
        } catch (RestClientException exception) {
            return lifecycleService.markCreditFailed(transactionId, exception.getMessage());
        }
    }

    public OutgoingTransfer get(String transactionId) {
        return lifecycleService.get(transactionId);
    }

    private void requireConfiguredCounterpart(String destinationBankId) {
        if (!bankProperties.counterpartId().equals(destinationBankId)) {
            throw new IllegalArgumentException(
                    "This simulation instance only supports counterpart " + bankProperties.counterpartId()
            );
        }
    }
}
