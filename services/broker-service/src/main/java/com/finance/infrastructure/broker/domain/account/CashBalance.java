package com.finance.infrastructure.broker.domain.account;

import com.finance.infrastructure.common.money.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

// 각 증권계좌의 결제완료 예수금과 주문·미결제 상태를 관리하는 엔티티
@Entity
@Table(name = "cash_balances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "securities_account_id", nullable = false, unique = true)
    private SecuritiesAccount account;

    // 실제 현금결제가 완료되어 증권사 고객원장에 반영된 예수금
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal settledAmount;

    // 아직 체결되지 않은 매수 주문을 위해 주문가능금액에서 예약한 금액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal orderLockedAmount;

    // 매수 체결 후 결제일에 지급해야 하는 금액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal pendingDebitAmount;

    // 매도 체결 후 결제일에 수령할 예정이며 신규 주문에는 사용할 수 있지만 아직 출금할 수 없는 금액
    @Column(nullable = false, precision = 19, scale = 0)
    @Getter(AccessLevel.NONE)
    private BigDecimal pendingCreditAmount;

    public CashBalance(SecuritiesAccount account) {
        this.account = Objects.requireNonNull(account, "securities account must not be null");
        this.settledAmount = Money.zero().amount();
        this.orderLockedAmount = Money.zero().amount();
        this.pendingDebitAmount = Money.zero().amount();
        this.pendingCreditAmount = Money.zero().amount();
    }

    // 외부 입금 등으로 결제완료 예수금을 증가시킨다.
    public Money deposit(Money amount) {
        requirePositive(amount);
        settledAmount = settled().add(amount).amount();
        return settled();
    }

    // 결제예정 의무와 주문예약을 제외한 출금가능금액 안에서 출금한다.
    public Money withdraw(Money amount) {
        requirePositive(amount);
        if (withdrawableAmount().amount().compareTo(amount.amount()) < 0) {
            throw new IllegalArgumentException("insufficient withdrawable cash");
        }
        settledAmount = settled().subtract(amount).amount();
        return settled();
    }

    // 매수 주문 접수 시 주문가능금액에서 최대 주문금액을 예약한다.
    public void lockForBuyOrder(Money amount) {
        requirePositive(amount);
        if (orderableAmount().amount().compareTo(amount.amount()) < 0) {
            throw new IllegalArgumentException("insufficient orderable cash");
        }
        orderLockedAmount = orderLocked().add(amount).amount();
    }

    // 주문 취소 또는 거부 시 미체결 수량에 해당하는 주문예약금액을 해제한다.
    public void releaseBuyOrderLock(Money amount) {
        orderLockedAmount = subtractRequired(orderLocked(), amount, "insufficient order-locked cash").amount();
    }

    /**
     * 매수 체결 수량에 해당하는 주문예약금액을 결제예정 지급액으로 전환한다.
     * 지정가보다 낮게 체결된 차액은 별도 잔액 이동 없이 주문가능금액으로 복원된다.
     */
    public void recordBuyExecution(Money reservedAmount, Money settlementAmount) {
        requirePositive(reservedAmount);
        requirePositive(settlementAmount);
        if (settlementAmount.amount().compareTo(reservedAmount.amount()) > 0) {
            throw new IllegalArgumentException("settlement amount exceeds reserved order amount");
        }
        Money newOrderLocked = subtractRequired(
                orderLocked(),
                reservedAmount,
                "insufficient order-locked cash"
        );
        Money newPendingDebit = pendingDebit().add(settlementAmount);
        orderLockedAmount = newOrderLocked.amount();
        pendingDebitAmount = newPendingDebit.amount();
    }

    // 매도 체결대금은 결제 전에도 신규 매수주문의 재원으로 사용할 수 있도록 수령예정액에 반영한다.
    public void recordSellExecution(Money settlementAmount) {
        requirePositive(settlementAmount);
        pendingCreditAmount = pendingCredit().add(settlementAmount).amount();
    }

    /**
     * 같은 결제일의 매수 지급액과 매도 수령액을 함께 결제완료 예수금에 반영한다.
     * 미결제 매도대금으로 다시 매수한 경우에도 수령과 지급을 순액으로 처리할 수 있다.
     */
    public void settle(Money debitAmount, Money creditAmount) {
        requireNonNegative(debitAmount, "settlement debit");
        requireNonNegative(creditAmount, "settlement credit");
        if (debitAmount.amount().signum() == 0 && creditAmount.amount().signum() == 0) {
            throw new IllegalArgumentException("settlement amount must not be zero");
        }
        if (pendingDebitAmount.compareTo(debitAmount.amount()) < 0) {
            throw new IllegalArgumentException("insufficient pending settlement debit");
        }
        if (pendingCreditAmount.compareTo(creditAmount.amount()) < 0) {
            throw new IllegalArgumentException("insufficient pending settlement credit");
        }

        Money newSettled = settled().add(creditAmount).subtract(debitAmount);
        if (newSettled.isNegative()) {
            throw new IllegalArgumentException("insufficient settled cash for settlement");
        }
        pendingDebitAmount = pendingDebitAmount.subtract(debitAmount.amount());
        pendingCreditAmount = pendingCreditAmount.subtract(creditAmount.amount());
        settledAmount = newSettled.amount();
    }

    public Money settled() {
        return new Money(settledAmount);
    }

    public Money orderLocked() {
        return new Money(orderLockedAmount);
    }

    public Money pendingDebit() {
        return new Money(pendingDebitAmount);
    }

    public Money pendingCredit() {
        return new Money(pendingCreditAmount);
    }

    // 결제완료 예수금과 미결제 매도대금에서 지급예정액과 주문예약액을 제외한 신규 매수가능금액
    public Money orderableAmount() {
        return settled()
                .add(pendingCredit())
                .subtract(pendingDebit())
                .subtract(orderLocked());
    }

    // 미결제 매도대금은 제외하고 현재 출금할 수 있는 결제완료 금액
    public Money withdrawableAmount() {
        BigDecimal amount = orderableAmount().amount()
                .max(BigDecimal.ZERO)
                .min(settledAmount);
        return Money.of(amount);
    }

    // 주문예약을 제외하고 체결까지 반영한 고객의 순현금자산
    public Money netAmount() {
        return settled().add(pendingCredit()).subtract(pendingDebit());
    }

    private static Money subtractRequired(Money current, Money amount, String message) {
        requirePositive(amount);
        Money result = current.subtract(amount);
        if (result.isNegative()) {
            throw new IllegalArgumentException(message);
        }
        return result;
    }

    private static void requirePositive(Money amount) {
        Objects.requireNonNull(amount, "cash amount must not be null");
        if (amount.amount().signum() <= 0) {
            throw new IllegalArgumentException("cash amount must be positive");
        }
    }

    private static void requireNonNegative(Money amount, String fieldName) {
        Objects.requireNonNull(amount, fieldName + " must not be null");
        if (amount.amount().signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
    }
}
