package com.finance.infrastructure.bank.service.transfer.internal;

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
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
* 같은 은행내의 이체를 처리한다
* 같은 은행내의 거래에서는 은행간 당좌예금의 변화도 없으며, 금융결제원에서 상계 및 청산할 필요도 없다
* 따라서 같은 은행내의 결제는 중앙은행 및 금융결제원과의 상호작용이 필요 없다.
* */
@Service
@RequiredArgsConstructor
public class InternalTransferService {

    private final BankProperties bankProperties; // 현재 실행주체의 기관정보
    private final DepositAccountRepository accountRepository;
    private final BankTransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;

    // 은행 내 이체는 하나의 트랜잭션 내에서 원자적으로 거래를 처리할 수 있다.
    @Transactional
    public TransferResult transfer(Long customerId, TransferCommand command) {
        // 송신 꼐좌와 수신 계좌를 기반으로 AccountId가 작은 계좌부터 lock을 획득함으로서 데드락을 방지한다.
        LockedAccounts accounts = lockAccountsInIdOrder(
                command.senderAccountNumber(),
                command.receiverAccountNumber()
        );
        // 송신 계좌가 실제 송신자가 소유한 계좌가 맞는지를 검사한다.
        requireOwnership(customerId, accounts.sender());

        // 송신 기관정보 조회
        InstitutionId institutionId = bankProperties.currentInstitutionId();

        // 송신 은행과 계좌번호정보를 담은 Record 생성
        AccountReference senderReference = new AccountReference(
                institutionId,
                accounts.sender().getAccountNumber()
        );

        // 수신 은행과 계좌번호정보를 담은 Record 생성
        AccountReference receiverReference = new AccountReference(
                institutionId,
                accounts.receiver().getAccountNumber()
        );

        // 이미 해당 Request Id에 해당하는 Transaction이 존재했는지를 검사한다.
        BankTransaction existing = transactionRepository.findByRequestId(command.requestId())
                .orElse(null);

        if (existing != null) {
            // Request Id가 같고, 내용도 같으면 동일한 이체요청으로 인식하고 멱등성을 보장한다.
            // Request Id가 같은데, 내용이 다르면 다른 이체요청인데 Request Id가 중복된것으로 보면서 새로운 Request Id를 발급해야한다는 예외를 발생시킨다.
            requireSameTransfer(existing, command, senderReference, receiverReference);
            return TransferResult.from(existing);
        }

        // 새로운 Transaction을 저장한다.
        BankTransaction transaction = transactionRepository.save(new BankTransaction(
                command.requestId(),
                BankTransactionType.INTERNAL_TRANSFER,
                senderReference,
                receiverReference,
                command.amount()
        ));

        // 송신자의 예금 감소, 수신자의 예금 증가
        Money senderBalance = accounts.sender().decrease(command.amount());
        Money receiverBalance = accounts.receiver().increase(command.amount());

        // 송신자 원장과 수신자 원장을 저장한다.
        ledgerRepository.saveAll(List.of(
                new Ledger(
                        transaction,
                        accounts.sender(),
                        BalanceChangeType.DECREASE,
                        command.amount(),
                        senderBalance
                ),
                new Ledger(
                        transaction,
                        accounts.receiver(),
                        BalanceChangeType.INCREASE,
                        command.amount(),
                        receiverBalance
                )
        ));

        // transaction이 성공하면 transaction의 상태를 complete로 변경한다.
        transaction.complete();
        return TransferResult.from(transaction);
    }

    // 송신자 계좌와 수신자 계좌에 대해서 accountId가 더 작은 계좌부터 lock을 획득한다.
    private LockedAccounts lockAccountsInIdOrder(
            String senderAccountNumber,
            String receiverAccountNumber
    ) {
        Long senderId = findAccountId(senderAccountNumber);
        Long receiverId = findAccountId(receiverAccountNumber);
        // 같은 계좌로의 이체는 예외가 발생한다.
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("sender and receiver accounts must be different");
        }

        Long firstId = Math.min(senderId, receiverId);
        Long secondId = Math.max(senderId, receiverId);
        DepositAccount first = lockAccount(firstId);
        DepositAccount second = lockAccount(secondId);

        // lock을 획득한 채 송신자 계좌와 수신자 계좌를 리턴한다.
        if (senderId.equals(firstId)) {
            return new LockedAccounts(first, second);
        }
        return new LockedAccounts(second, first);
    }

    private Long findAccountId(String accountNumber) {
        return accountRepository.findIdByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "account not found: " + accountNumber
                ));
    }

    private DepositAccount lockAccount(Long accountId) {
        return accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "account not found: " + accountId
                ));
    }

    // 송신계좌가 송신자가 소유한 계좌가 맞는지를 검사한다.
    private static void requireOwnership(Long customerId, DepositAccount sender) {
        if (!sender.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "sender account does not belong to the customer"
            );
        }
    }

    // 이미 동일한 이체요청을 이전에 처리하고 있었는지를 검사한다.
    private static void requireSameTransfer(
            BankTransaction existing,
            TransferCommand command,
            AccountReference sender,
            AccountReference receiver
    ) {
        // 동일한 이체 요청을 이전에도 처리하고 있었는지를 검사한다.
        boolean sameTransfer = existing.getType() == BankTransactionType.INTERNAL_TRANSFER
                && existing.sender().equals(sender)
                && existing.receiver().equals(receiver)
                && existing.amount().equals(command.amount());
        // 만약 request id가 동일한데, 이체정보가 다르다면 서로 다른 이체요청인데 request id가 중복된 것이므로 새로운 request id를 발급받아야 한다.
        if (!sameTransfer) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "request id was already used for a different transfer: " + command.requestId()
            );
        }
    }

    private record LockedAccounts(DepositAccount sender, DepositAccount receiver) {
    }
}
