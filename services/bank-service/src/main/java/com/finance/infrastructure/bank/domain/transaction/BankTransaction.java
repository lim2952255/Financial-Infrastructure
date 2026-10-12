package com.finance.infrastructure.bank.domain.transaction;

import com.finance.infrastructure.bank.domain.account.AccountReference;
import com.finance.infrastructure.common.institution.InstitutionId;
import com.finance.infrastructure.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "bank_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String requestId;

    // KFTC가 발급한 지급지시 식별자. 은행 간 거래에서 중복 입금과 중복 처리를 방지한다.
    @Column(unique = true, length = 64)
    private String paymentInstructionId;

    @Column(nullable = false, updatable = false, length = 64)
    @Convert(converter = InstitutionIdConverter.class) // InstitutionId 레코드를 DB에 적절히 저장하기 위해 컨버터 설정
    @Getter(AccessLevel.NONE)
    private InstitutionId senderInstitutionId;

    @Column(nullable = false, updatable = false, length = 32)
    @Getter(AccessLevel.NONE)
    private String senderAccountNumber;

    @Column(nullable = false, updatable = false, length = 64)
    @Convert(converter = InstitutionIdConverter.class) // InstitutionId 레코드를 DB에 적절히 저장하기 위해 컨버터 설정
    @Getter(AccessLevel.NONE)
    private InstitutionId receiverInstitutionId;

    @Column(nullable = false, updatable = false, length = 32)
    @Getter(AccessLevel.NONE)
    private String receiverAccountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 32)
    private BankTransactionType type; // 이체 유형

    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal amountValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status; // 거래 상태

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    // 고객 이체 완료와 별개로 BOK의 은행 간 최종 결제가 끝난 시각이다.
    private LocalDateTime settlementCompletedAt;

    // 이 거래가 포함된 KFTC 청산주기와 BOK 결제배치를 추적한다.
    @Column(length = 64)
    private String clearingCycleId;

    @Column(length = 64)
    private String settlementBatchId;

    @Column(length = 255)
    private String failureReason; // 거래 실패 이유

    public BankTransaction(
            String requestId,
            BankTransactionType type,
            AccountReference sender,
            AccountReference receiver,
            Money amount
    ) {
        this.requestId = requireRequestId(requestId);
        this.type = Objects.requireNonNull(type, "transaction type must not be null");
        Objects.requireNonNull(sender, "sender account reference must not be null");
        Objects.requireNonNull(receiver, "receiver account reference must not be null");
        requireDifferentAccounts(sender, receiver);
        requireValidInstitutions(type, sender, receiver);
        requireValidAmount(amount);
        this.senderInstitutionId = sender.institutionId();
        this.senderAccountNumber = sender.accountNumber();
        this.receiverInstitutionId = receiver.institutionId();
        this.receiverAccountNumber = receiver.accountNumber();
        this.amountValue = amount.amount();
        this.status = TransactionStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    // 거래 성공  처리
    public void complete() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("only pending transactions can be completed");
        }
        status = TransactionStatus.COMPLETED;
        completedAt = LocalDateTime.now();
    }

    // 거래 실패 처리
    public void fail(String reason) {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException("only pending transactions can fail");
        }
        failureReason = requireFailureReason(reason); // 거래 실패 이유 저장
        status = TransactionStatus.FAILED;
        completedAt = LocalDateTime.now();
    }

    public Money amount() {
        return new Money(amountValue);
    }

    public void assignPaymentInstructionId(String paymentInstructionId) {
        if (type == BankTransactionType.INTERNAL_TRANSFER) {
            throw new IllegalStateException(
                    "internal transfer cannot have a payment instruction id"
            );
        }
        if (this.paymentInstructionId != null) {
            throw new IllegalStateException("payment instruction id is already assigned");
        }
        this.paymentInstructionId = requirePaymentInstructionId(paymentInstructionId);
    }

    public void confirmInterbankSettlement(
            String clearingCycleId,
            String settlementBatchId
    ) {
        if (type == BankTransactionType.INTERNAL_TRANSFER) {
            throw new IllegalStateException("internal transfer does not require settlement");
        }
        if (status != TransactionStatus.COMPLETED) {
            throw new IllegalStateException("only completed transactions can be settled");
        }

        String normalizedCycleId = requireText(clearingCycleId, "clearing cycle id");
        String normalizedBatchId = requireText(settlementBatchId, "settlement batch id");
        if (settlementCompletedAt != null) {
            if (!Objects.equals(this.clearingCycleId, normalizedCycleId)
                    || !Objects.equals(this.settlementBatchId, normalizedBatchId)) {
                throw new IllegalStateException(
                        "interbank transaction was already settled by another batch"
                );
            }
            return;
        }

        this.clearingCycleId = normalizedCycleId;
        this.settlementBatchId = normalizedBatchId;
        settlementCompletedAt = LocalDateTime.now();
    }

    // AccountReference는 어떤 기관의 계좌인지 정보를 나타낸다.
    public AccountReference sender() {
        return new AccountReference(senderInstitutionId, senderAccountNumber);
    }

    public AccountReference receiver() {
        return new AccountReference(receiverInstitutionId, receiverAccountNumber);
    }

    private static String requireRequestId(String requestId) {
        Objects.requireNonNull(requestId, "request id must not be null");
        String normalized = requestId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("request id must not be blank");
        }
        return normalized;
    }

    private static String requirePaymentInstructionId(String paymentInstructionId) {
        Objects.requireNonNull(
                paymentInstructionId,
                "payment instruction id must not be null"
        );
        String normalized = paymentInstructionId.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("payment instruction id must not be blank");
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String requireFailureReason(String reason) {
        Objects.requireNonNull(reason, "failure reason must not be null");
        String normalized = reason.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("failure reason must not be blank");
        }
        return normalized;
    }

    private static void requireDifferentAccounts(
            AccountReference sender,
            AccountReference receiver
    ) {
        if (sender.equals(receiver)) {
            throw new IllegalArgumentException("sender and receiver accounts must be different");
        }
    }

    // BankTransactionType이 실제 거래유형이랑 맞게 설정되어 있는지를 검사한다.
    private static void requireValidInstitutions(
            BankTransactionType type,
            AccountReference sender,
            AccountReference receiver
    ) {
        boolean sameInstitution = sender.institutionId().equals(receiver.institutionId());
        if (type == BankTransactionType.INTERNAL_TRANSFER && !sameInstitution) {
            throw new IllegalArgumentException("internal transfers require the same institution");
        }
        if (type != BankTransactionType.INTERNAL_TRANSFER && sameInstitution) {
            throw new IllegalArgumentException("interbank transfers require different institutions");
        }
    }

    private static void requireValidAmount(Money amount) {
        Objects.requireNonNull(amount, "transaction amount must not be null");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("transaction amount must be positive");
        }
    }
}
