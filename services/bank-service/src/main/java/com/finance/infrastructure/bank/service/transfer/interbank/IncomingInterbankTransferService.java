package com.finance.infrastructure.bank.service.transfer.interbank;

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
import com.finance.infrastructure.bank.service.transfer.TransferResult;
import com.finance.infrastructure.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// 수신은행에서 KFTC 지급지시를 멱등성을 보장하며 처리하고, 수신계좌의 예금을 증가시킨다.
@Service
@RequiredArgsConstructor
public class IncomingInterbankTransferService {

    private final BankProperties bankProperties; // 수신은행 정보와 KFTC url를 담고 있는 객체이다.
    private final DepositAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    // KFTC로부터 받은 입금지시를 수신은행에서 처리하는 메서드
    @Transactional
    public TransferResult credit(IncomingInterbankTransferCommand command) {
        requireCurrentBank(command);

        // 이미 해당 입금지시를 이전에 받은적이 있는지를 검사한다.
        BankTransaction existing = transactionRepository
                .findByPaymentInstructionId(command.instructionId())
                .orElse(null);
        // 만약 해당 입금지시를 받은적이 있다면, 이전에 이미 처리되고 있던 입금지시인지, 아니면 우연히 instructionId가 중복된 것인지를 검사하고, 예외 또는 이전 입금처리결과를 리턴한다.
        if (existing != null) {
            requireSameTransfer(existing, command);
            return TransferResult.from(existing);
        }

        // 수신계좌를 lock을 획득한 채로 조회한다.
        DepositAccount receiver = lockAccount(command.receiverAccountNumber());

        // 송신기관과 송신계좌 정보를 담은 AccountReference 레코드 생성
        AccountReference senderReference = new AccountReference(
                command.senderInstitutionId(),
                command.senderAccountNumber()
        );
        // 수신기관과 수신계좌 정보를 담은 AccountReference 레코드 생성
        AccountReference receiverReference = new AccountReference(
                command.receiverInstitutionId(),
                receiver.getAccountNumber()
        );

        // requestId는 고객이 은행에 이체요청을 보낼때, 해당 이체요청을 멱등성을 보장하기 위한 멱등성 키이다. 또한 송신은행이 KFTC에 지급지시 요청을 보낼때에도 requestId를 활용하여 멱등성을 보장한다.
        // instructionId는 KFTC가 수신은행에 입금요청을 보낼때, 해당 입금요청을 멱등성을 보장하기 위한 멱등성 키이다.
        // 따라서 KFTC가 수신은행에 입금요청을 보낼때에는 고객이 은행에 이체요청을 보낸것이 아니기 때문에 requestId가 없지만, BankTransaction에서 requestId가 notnull이기 때문에 instructionId를 넣어준다.
        BankTransaction transaction = new BankTransaction(
                command.instructionId(),
                BankTransactionType.INTERBANK_INCOMING,
                senderReference,
                receiverReference,
                command.amount()
        );

        // transaction에 KFTC가 발급한 멱등성 키를 등록한다.
        transaction.assignPaymentInstructionId(command.instructionId());
        transactionRepository.save(transaction);

        // 수신계좌의 잔액을 증가시키고, 입금 원장을 기록한다.
        Money balanceAfter = receiver.increase(command.amount());
        ledgerRepository.save(new Ledger(
                transaction,
                receiver,
                BalanceChangeType.INCREASE,
                command.amount(),
                balanceAfter
        ));
        // transaction의 상태를 complete로 update하고, 이체요청 처리 결과를 리턴한다.
        transaction.complete();
        return TransferResult.from(transaction);
    }

    // 현재 수신기관과 입금지시요청에 담긴 수신기관이 일치하는지를 검사한다.
    private void requireCurrentBank(IncomingInterbankTransferCommand command) {
        if (!bankProperties.currentInstitutionId().equals(command.receiverInstitutionId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "payment instruction was sent to the wrong bank"
            );
        }
    }

    // 수신계좌에 lock을 획득한 채로 조회한다.
    private DepositAccount lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "receiver account not found: " + accountNumber
                ));
    }

    // 만약 해당 입금지시를 받은적이 있다면, 이전에 이미 처리되고 있던 입금지시인지, 아니면 우연히 instructionId가 중복된 것인지를 검사하고, 예외 또는 이전 입금처리결과를 리턴한다.
    private static void requireSameTransfer(
            BankTransaction existing,
            IncomingInterbankTransferCommand command
    ) {
        boolean sameTransfer = existing.getType() == BankTransactionType.INTERBANK_INCOMING
                && existing.sender().institutionId().equals(command.senderInstitutionId())
                && existing.sender().accountNumber().equals(command.senderAccountNumber())
                && existing.receiver().institutionId().equals(command.receiverInstitutionId())
                && existing.receiver().accountNumber().equals(command.receiverAccountNumber())
                && existing.amount().equals(command.amount());
        if (!sameTransfer) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "instruction id was already used for a different transfer"
            );
        }
    }
}
