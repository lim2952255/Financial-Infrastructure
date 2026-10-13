package com.finance.infrastructure.broker.domain.security;

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

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/**
 * 증권사가 고객 주문과 보유종목 관리에 사용하는 거래 가능 종목정보를 담은 엔티티
 *
 * 상장과 시장운영에 대한 최종 권위는 KRX에 있으며, Broker는 KRX가 제공한 종목정보를 자신의 업무에 필요한 형태로 보관한다.
 */
@Entity
@Table(name = "tradable_securities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradableSecurity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // KRX에서 설정한 종목코드
    @Column(nullable = false, unique = true, updatable = false, length = 32)
    private String securityCode;

    @Column(nullable = false, length = 100)
    private String name;

    // 종목의 시장정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Market market;

    // 해당 종목이 현재 거래가능한지 상태를 나타내는 상태정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TradingStatus tradingStatus;

    @Column(nullable = false)
    private long orderUnit;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public TradableSecurity(
            String securityCode,
            String name,
            Market market,
            long orderUnit
    ) {
        this.securityCode = requireSecurityCode(securityCode);
        this.name = requireText(name, "security name");
        this.market = Objects.requireNonNull(market, "market must not be null");
        this.orderUnit = requirePositiveOrderUnit(orderUnit);
        this.tradingStatus = TradingStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    // 증권사에서 관리하는 종목정보는 KRX에서 제공하는 종목정보에 의존하기 때문에, KRX에서 제공하는 종목정보가 달라지면 증권사에서 관리하는 종목정보도 update해야 한다.
    public void changeName(String name) {
        this.name = requireText(name, "security name");
        touch();
    }

    public void changeMarket(Market market) {
        requireNotDelisted();
        this.market = Objects.requireNonNull(market, "market must not be null");
        touch();
    }

    public void changeOrderUnit(long orderUnit) {
        requireNotDelisted();
        this.orderUnit = requirePositiveOrderUnit(orderUnit);
        touch();
    }

    public void suspendTrading() {
        requireStatus(TradingStatus.ACTIVE);
        tradingStatus = TradingStatus.SUSPENDED;
        touch();
    }

    public void resumeTrading() {
        requireStatus(TradingStatus.SUSPENDED);
        tradingStatus = TradingStatus.ACTIVE;
        touch();
    }

    public void delist() {
        if (tradingStatus == TradingStatus.DELISTED) {
            return;
        }
        tradingStatus = TradingStatus.DELISTED;
        touch();
    }

    public void validateOrder(long quantity) {
        if (tradingStatus != TradingStatus.ACTIVE) {
            throw new IllegalStateException("security is not tradable: " + tradingStatus);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("order quantity must be positive");
        }
        if (quantity % orderUnit != 0) {
            throw new IllegalArgumentException("order quantity must be a multiple of the order unit");
        }
    }

    private void requireNotDelisted() {
        if (tradingStatus == TradingStatus.DELISTED) {
            throw new IllegalStateException("delisted security cannot be changed");
        }
    }

    private void requireStatus(TradingStatus expected) {
        if (tradingStatus != expected) {
            throw new IllegalStateException(
                    "trading status must be " + expected + " but was " + tradingStatus
            );
        }
    }

    // 종목정보 수정일자를 update
    private void touch() {
        updatedAt = LocalDateTime.now();
    }

    private static long requirePositiveOrderUnit(long orderUnit) {
        if (orderUnit <= 0) {
            throw new IllegalArgumentException("order unit must be positive");
        }
        return orderUnit;
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
