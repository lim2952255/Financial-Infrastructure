package com.finance.infrastructure.ksd.domain.security;

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
 * KSD가 참가자별 보유수량과 증권결제에 사용하는 종목정보를 담은 엔티티.
 *
 * 상장과 시장운영에 대한 최종 권위는 KRX에 있으며,
 * KSD는 KRX가 제공한 종목정보를 자신의 업무에 필요한 형태로 보관한다.
 */
@Entity
@Table(name = "depository_securities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositorySecurity {

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

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public DepositorySecurity(String securityCode, String name, Market market) {
        this.securityCode = requireText(securityCode, "security code").toUpperCase(Locale.ROOT);
        this.name = requireText(name, "security name");
        this.market = Objects.requireNonNull(market, "market must not be null");
        this.tradingStatus = TradingStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    // KSD가 관리하는 종목정보는 KRX에서 제공하는 종목정보에 의존하기 때문에, KRX의 종목명이 변경되면 KSD의 종목정보에도 반영한다.
    public void changeName(String name) {
        this.name = requireText(name, "security name");
        touch();
    }

    // KRX에서 제공한 시장정보가 변경되면 KSD가 보관하는 시장정보에도 반영한다.
    public void changeMarket(Market market) {
        requireNotDelisted();
        this.market = Objects.requireNonNull(market, "market must not be null");
        touch();
    }

    // KRX에서 거래정지된 종목의 상태를 KSD에도 반영한다.
    public void suspendTrading() {
        requireStatus(TradingStatus.ACTIVE);
        tradingStatus = TradingStatus.SUSPENDED;
        touch();
    }

    // KRX에서 거래가 재개된 종목의 상태를 KSD에도 반영한다.
    public void resumeTrading() {
        requireStatus(TradingStatus.SUSPENDED);
        tradingStatus = TradingStatus.ACTIVE;
        touch();
    }

    // KRX에서 상장폐지된 종목의 상태를 KSD에도 반영한다.
    public void delist() {
        if (tradingStatus == TradingStatus.DELISTED) {
            return;
        }
        tradingStatus = TradingStatus.DELISTED;
        touch();
    }

    public boolean isActive() {
        return tradingStatus == TradingStatus.ACTIVE;
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

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
