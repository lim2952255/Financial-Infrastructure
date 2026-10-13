package com.finance.infrastructure.krx.domain.security;

import com.finance.infrastructure.common.money.Money;
import jakarta.persistence.Column;
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
import java.util.Locale;
import java.util.Objects;

/**
 * KRX가 상장정보와 거래 가능 여부를 원본으로 관리하는 종목정보 엔티티
 * Broker와 KSD에서 사용하는 종목정보는 KRX가 관리하는 상장종목 정보를 각 업무에 맞게 보관한 정보이다.
 */
@Entity
@Table(name = "listed_securities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ListedSecurity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KRX가 상장종목에 발급한 종목코드
    @Column(nullable = false, unique = true, updatable = false, length = 32)
    private String securityCode;

    // 상장종목 이름
    @Column(nullable = false, length = 100)
    private String name;

    // 종목이 상장된 시장정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Market market;

    // 종목의 현재 거래 가능 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TradingStatus tradingStatus;

    // 주문수량이 따라야 하는 최소 거래단위. 즉 한번에 주문할 수 있는 주식수량의 단위이다.
    @Column(nullable = false)
    private long orderUnit;

    // 주문가격이 따라야 하는 최소 호가단위. 즉 주문가격을 얼마씩 움직일 수 있는지를 나타낸다.
    @Column(nullable = false)
    private long tickSize;

    // 종목정보를 등록한 시각
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 종목정보나 거래상태가 마지막으로 변경된 시각
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ListedSecurity(
            String securityCode,
            String name,
            Market market,
            long orderUnit,
            long tickSize
    ) {
        this.securityCode = requireSecurityCode(securityCode);
        this.name = requireText(name, "security name");
        this.market = Objects.requireNonNull(market, "market must not be null");
        this.orderUnit = requirePositive(orderUnit, "order unit");
        this.tickSize = requirePositive(tickSize, "tick size");
        this.tradingStatus = TradingStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    // 상장종목 이름을 변경한다.
    public void changeName(String name) {
        requireNotDelisted();
        this.name = requireText(name, "security name");
        touch();
    }

    // 종목이 소속된 시장정보를 변경한다.
    public void changeMarket(Market market) {
        requireNotDelisted();
        this.market = Objects.requireNonNull(market, "market must not be null");
        touch();
    }

    // 주문수량의 최소 거래단위를 변경한다.
    public void changeOrderUnit(long orderUnit) {
        requireNotDelisted();
        this.orderUnit = requirePositive(orderUnit, "order unit");
        touch();
    }

    // 주문가격의 최소 호가단위를 변경한다.
    public void changeTickSize(long tickSize) {
        requireNotDelisted();
        this.tickSize = requirePositive(tickSize, "tick size");
        touch();
    }

    // 정상 거래 중인 종목의 거래를 일시 정지한다.
    public void suspendTrading() {
        requireStatus(TradingStatus.ACTIVE);
        tradingStatus = TradingStatus.SUSPENDED;
        touch();
    }

    // 거래가 정지된 종목을 다시 거래 가능한 상태로 변경한다.
    public void resumeTrading() {
        requireStatus(TradingStatus.SUSPENDED);
        tradingStatus = TradingStatus.ACTIVE;
        touch();
    }

    // 종목을 상장폐지 상태로 변경하며 이미 상장폐지된 경우에는 상태를 유지한다.
    public void delist() {
        if (tradingStatus == TradingStatus.DELISTED) {
            return;
        }
        tradingStatus = TradingStatus.DELISTED;
        touch();
    }

    // 현재 주문을 접수할 수 있는 거래 가능 종목인지 확인한다.
    public boolean isTradable() {
        return tradingStatus == TradingStatus.ACTIVE;
    }

    // 주문수량이 거래단위에 맞고 주문가격이 호가단위에 맞는지 검사한다.
    public void validateOrder(long quantity, Money price) {
        Objects.requireNonNull(price, "order price must not be null");
        if (!isTradable()) {
            throw new IllegalStateException("security is not tradable: " + tradingStatus);
        }
        requirePositive(quantity, "order quantity");
        if (quantity % orderUnit != 0) {
            throw new IllegalArgumentException("order quantity must be a multiple of the order unit");
        }

        BigDecimal amount = price.amount();
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("order price must be positive");
        }
        if (amount.remainder(BigDecimal.valueOf(tickSize)).signum() != 0) {
            throw new IllegalArgumentException("order price must be a multiple of the tick size");
        }
    }

    // 상장폐지된 종목의 기본정보가 변경되는 것을 방지한다.
    private void requireNotDelisted() {
        if (tradingStatus == TradingStatus.DELISTED) {
            throw new IllegalStateException("delisted security cannot be changed");
        }
    }

    // 종목상태가 잘못변경되는것을 방지한다.
    private void requireStatus(TradingStatus expected) {
        if (tradingStatus != expected) {
            throw new IllegalStateException(
                    "trading status must be " + expected + " but was " + tradingStatus
            );
        }
    }

    // 종목정보가 마지막으로 변경된 시각을 update한다.
    private void touch() {
        updatedAt = LocalDateTime.now();
    }

    private static long requirePositive(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static String requireSecurityCode(String securityCode) {
        return requireText(securityCode, "security code").toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
