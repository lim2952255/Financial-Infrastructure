package com.finance.infrastructure.kftc.domain.payment;

import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.clearing.ClearingCycleStatus;
import com.finance.infrastructure.kftc.domain.participant.ClearingParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// 송금은행으로부터 접수해 수취은행으로 중계하는 개별 지급지시
@Entity
@Table(
        name = "payment_instructions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_instruction_sender_request",
                columnNames = {"sender_participant_id", "request_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 지급지시 식별자 키
    @Column(nullable = false, unique = true, updatable = false, length = 64)
    private String instructionId;

    // 송신기관에서 사용하는 멱등성 키
    @Column(name = "request_id", nullable = false, updatable = false, length = 64)
    private String requestId;

    // 송신기관
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_participant_id", nullable = false, updatable = false)
    private ClearingParticipant senderParticipant;

    @Column(nullable = false, updatable = false, length = 32)
    private String senderAccountNumber;

    // 수신기관
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_participant_id", nullable = false, updatable = false)
    private ClearingParticipant receiverParticipant;

    @Column(nullable = false, updatable = false, length = 32)
    private String receiverAccountNumber;

    // 이체 금액
    @Column(nullable = false, updatable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal amountValue;

    // 지급지시 처리상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentInstructionStatus status;

    // 지급지시들은 특정 청산주기마다 청산되어 순포지션계산에 활용된다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clearing_cycle_id")
    private ClearingCycle clearingCycle;

    // KFTC가 지급지시를 접수한 시간
    @Column(nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    // KFTC가 지급지시를 수신기관으로 전송한 시간
    private LocalDateTime forwardedAt;
    // 수신기관에서 수취인 계좌의 입금과 원장 기록을 완료했다는 정보를 KFTC에 전달한 시간
    private LocalDateTime receiverCreditedAt;
    // KFTC 청산 후 BOK 당좌예금계좌에서 은행 간 최종 결제가 완료된 시간
    private LocalDateTime settledAt;

    // 지급지시 처리실패 이유
    @Column(length = 255)
    private String failureReason;

    public PaymentInstruction(
            String instructionId,
            String requestId,
            ClearingParticipant senderParticipant,
            String senderAccountNumber,
            ClearingParticipant receiverParticipant,
            String receiverAccountNumber,
            Money amount
    ) {
        this.instructionId = requireIdentifier(instructionId, "instruction id");
        this.requestId = requireIdentifier(requestId, "request id");
        this.senderParticipant = requireActiveParticipant(senderParticipant, "sender");
        this.receiverParticipant = requireActiveParticipant(receiverParticipant, "receiver");
        requireDifferentParticipants(senderParticipant, receiverParticipant);
        this.senderAccountNumber = requireIdentifier(senderAccountNumber, "sender account number");
        this.receiverAccountNumber = requireIdentifier(
                receiverAccountNumber,
                "receiver account number"
        );
        requirePositiveAmount(amount);
        this.amountValue = amount.amount();
        this.status = PaymentInstructionStatus.RECEIVED;
        this.receivedAt = LocalDateTime.now();
    }

    public Money amount() {
        return new Money(amountValue);
    }

    // 지급지시의 상태를 변경하는 메서드
    public void forward() {
        requireStatus(PaymentInstructionStatus.RECEIVED);
        status = PaymentInstructionStatus.FORWARDED;
        forwardedAt = LocalDateTime.now();
    }

    public void confirmReceiverCredit() {
        requireStatus(PaymentInstructionStatus.FORWARDED);
        status = PaymentInstructionStatus.RECEIVER_CREDITED;
        receiverCreditedAt = LocalDateTime.now();
    }

    // 특정 청산 주기에 지급지시를 현재 열려있는 청산 주기에 포함시킨다. 이후에 해당 지급지시정보를 활용하여 기관별 순포지션을 계산한다.
    public void assignTo(ClearingCycle clearingCycle) {
        requireStatus(PaymentInstructionStatus.RECEIVER_CREDITED);
        Objects.requireNonNull(clearingCycle, "clearing cycle must not be null");
        if (!clearingCycle.isOpen()) {
            throw new IllegalStateException("clearing cycle must be open");
        }
        if (this.clearingCycle != null) {
            throw new IllegalStateException("payment instruction already belongs to a clearing cycle");
        }
        this.clearingCycle = clearingCycle;
    }

    public void clear() {
        requireStatus(PaymentInstructionStatus.RECEIVER_CREDITED);
        if (clearingCycle == null
                || clearingCycle.getStatus() != ClearingCycleStatus.CALCULATED) {
            throw new IllegalStateException("clearing cycle must be calculated");
        }
        status = PaymentInstructionStatus.CLEARED;
    }

    public void settle() {
        requireStatus(PaymentInstructionStatus.CLEARED);
        status = PaymentInstructionStatus.SETTLED;
        settledAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        if (status != PaymentInstructionStatus.RECEIVED
                && status != PaymentInstructionStatus.FORWARDED) {
            throw new IllegalStateException("credited payment instruction cannot be rejected");
        }
        failureReason = requireIdentifier(reason, "failure reason");
        status = PaymentInstructionStatus.REJECTED;
    }

    private void requireStatus(PaymentInstructionStatus expected) {
        if (status != expected) {
            throw new IllegalStateException(
                    "payment instruction must be " + expected + " but was " + status
            );
        }
    }

    private static ClearingParticipant requireActiveParticipant(
            ClearingParticipant participant,
            String role
    ) {
        Objects.requireNonNull(participant, role + " participant must not be null");
        if (!participant.isActive()) {
            throw new IllegalArgumentException(role + " participant must be active");
        }
        return participant;
    }

    // 지급지시는 서로다른 기관간의 거래에서만 활용한다. 같은 기관내의 이체는 해당 기관내부에서 스스로  처리한다.
    private static void requireDifferentParticipants(
            ClearingParticipant sender,
            ClearingParticipant receiver
    ) {
        if (sender.getInstitutionId().equals(receiver.getInstitutionId())) {
            throw new IllegalArgumentException(
                    "payment instruction requires different institutions"
            );
        }
    }

    private static void requirePositiveAmount(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private static String requireIdentifier(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
