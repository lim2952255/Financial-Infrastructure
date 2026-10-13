package com.finance.infrastructure.krx.domain.matching;

import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.krx.domain.order.ExchangeOrder;
import com.finance.infrastructure.krx.domain.order.OrderSide;

import java.util.Objects;

/**
 * KRX의 주문 매칭 엔진이 매칭한 결과를 담는 record
 * 하나의 Match는 서로 교차된 매수/매도 주문과 해당 매칭에서 결정된 체결가격 및 체결수량을 담는다.
 */
public record Match(
        ExchangeOrder buyOrder, // 체결수량만큼 매수 처리된 주문
        ExchangeOrder sellOrder, // 체결수량만큼 매도 처리된 주문
        Money price, // 먼저 접수된 주문의 지정가격을 기준으로 결정된 체결가격
        long quantity// 이번 매칭으로 체결된 수량
) {

    // 매칭 결과가 올바른 주문 방향과 양수의 가격·수량을 가지는지 검사한다.
    public Match {
        Objects.requireNonNull(buyOrder, "buy order must not be null");
        Objects.requireNonNull(sellOrder, "sell order must not be null");
        Objects.requireNonNull(price, "match price must not be null");
        if (buyOrder.getSide() != OrderSide.BUY) {
            throw new IllegalArgumentException("buy order must have BUY side");
        }
        if (sellOrder.getSide() != OrderSide.SELL) {
            throw new IllegalArgumentException("sell order must have SELL side");
        }
        if (price.amount().signum() <= 0) {
            throw new IllegalArgumentException("match price must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("match quantity must be positive");
        }
    }
}
