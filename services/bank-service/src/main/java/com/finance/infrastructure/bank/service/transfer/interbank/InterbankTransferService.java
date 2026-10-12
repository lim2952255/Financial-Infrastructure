package com.finance.infrastructure.bank.service.transfer.interbank;

import com.finance.infrastructure.bank.client.kftc.KftcClient;
import com.finance.infrastructure.bank.client.kftc.KftcClient.PaymentInstructionResponse;
import com.finance.infrastructure.bank.config.BankProperties;
import com.finance.infrastructure.bank.domain.account.AccountReference;
import com.finance.infrastructure.bank.domain.account.DepositAccount;
import com.finance.infrastructure.bank.domain.ledger.BalanceChangeType;
import com.finance.infrastructure.bank.domain.ledger.Ledger;
import com.finance.infrastructure.bank.domain.transaction.BankTransaction;
import com.finance.infrastructure.bank.domain.transaction.BankTransactionType;
import com.finance.infrastructure.bank.repository.BankTransactionRepository;
import com.finance.infrastructure.bank.repository.DepositAccountRepository;
import com.finance.infrastructure.bank.repository.LedgerRepository;
import com.finance.infrastructure.bank.service.transfer.TransferCommand;
import com.finance.infrastructure.bank.service.transfer.TransferResult;
import com.finance.infrastructure.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 은행 간 이체의 경우에는 송신은행에서 고객 예금을 출금하고, KFTC에 은행 간 지급지시를 전달한다.
@Service
@RequiredArgsConstructor
public class InterbankTransferService {

    private final BankProperties bankProperties; // 현재 기관의 institutionId와 KFTC의 url정보를 담고 있는 객체
    private final DepositAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final KftcClient kftcClient; // KFTC에 지급지시 요청을 보내고, 처리결과를 받는 클라이언트

    @Transactional
    public TransferResult transfer(Long customerId, TransferCommand command) {
        // 송신 기관과 계좌졍보를 담은 AccountReference 생성
        AccountReference senderReference = new AccountReference(
                bankProperties.currentInstitutionId(),
                command.senderAccountNumber()
        );
        // 수신 기관과 계좌정보를 담은 ACcountReference 생성
        AccountReference receiverReference = new AccountReference(
                command.receiverInstitutionId(),
                command.receiverAccountNumber()
        );

        // 만약 동일한 requestId를 이미 받은적이 있다면, 이를 다시 수행하지 않고, 예외 또는 이전에 처리하던 requestId의 거래정보를 리턴한다.(멱등성 보장)
        BankTransaction existing = transactionRepository.findByRequestId(command.requestId())
                .orElse(null);
        if (existing != null) {
            requireSameTransfer(existing, command, senderReference, receiverReference);
            return TransferResult.from(existing);
        }

        // 송신계좌를 lock을 획득한 채로 조회한다.
        DepositAccount sender = lockAccount(command.senderAccountNumber());
        // 이체요청을 보낸 고객이 소유한 계좌인지를 검사한다.
        requireOwnership(customerId, sender);

        // 새로운 Transaction 정보를 저장한다.
        BankTransaction transaction = transactionRepository.save(new BankTransaction(
                command.requestId(),
                BankTransactionType.INTERBANK_OUTGOING,
                senderReference,
                receiverReference,
                command.amount()
        ));

        // 외부 지급지시를 보내기 전에 송신자의 예금을 출금처리하고, 출금원장을 기록한다. (멱등성 방지)
        Money balanceAfter = sender.decrease(command.amount());
        ledgerRepository.save(new Ledger(
                transaction,
                sender,
                BalanceChangeType.DECREASE,
                command.amount(),
                balanceAfter
        ));

        // KFTC에 지급지시 요청을 보내고, KFTC의 지급지시 처리 결과를 받는다.
        PaymentInstructionResponse response = kftcClient.createPaymentInstruction(command);
        // transaction에 KFTC가 발급한 instructionId를 저장한다.(멱등성 방지를 위해 KFTC가 발급한 지급지시 식별자)
        transaction.assignPaymentInstructionId(response.instructionId());
        // receiverCredited()가 true이면 수신기관에서 입금처리를 성공적으로 처리했다는 의미이기 때문에 transaction의 상태를 complete로 변경한다.
        if (response.receiverCredited()) {
            transaction.complete();
        }

        // 이체 결과 정보를 리턴한다.
        return TransferResult.from(transaction);
    }

    // 송신계좌를 lock을 획득한 채로 조회한다.
    private DepositAccount lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "account not found: " + accountNumber
                ));
    }

    // 이체 요청을 보낸 고객 명의의 송금계좌가 맞는지를 검사한다.
    private static void requireOwnership(Long customerId, DepositAccount sender) {
        if (!sender.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "sender account does not belong to the customer"
            );
        }
    }

    // 같은 requestId로 들어온 이체요청이 이전에 이미 처리되고 있던 것인지 확인하고, 아니라면 새로운 requestId를 요청하라는 예외를 발생시킨다.
    private static void requireSameTransfer(
            BankTransaction existing,
            TransferCommand command,
            AccountReference sender,
            AccountReference receiver
    ) {
        boolean sameTransfer = existing.getType() == BankTransactionType.INTERBANK_OUTGOING
                && existing.sender().equals(sender)
                && existing.receiver().equals(receiver)
                && existing.amount().equals(command.amount());
        if (!sameTransfer) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "request id was already used for a different transfer: " + command.requestId()
            );
        }
    }
}
