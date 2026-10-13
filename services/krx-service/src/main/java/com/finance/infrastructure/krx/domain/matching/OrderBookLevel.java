package com.finance.infrastructure.krx.domain.matching;

import com.finance.infrastructure.common.money.Money;

import java.util.Objects;

/**
 * 호가창에서 같은 가격에 대기 중인 주문들을 하나로 집계해서 보여주는 조회용 record
 * 즉 하나의 호가에 대한 주문 수량 및 주문건수를 나타내는 record
 */
public record OrderBookLevel(
        Money price, // 집계 대상 호가
        long totalQuantity, // 해당 가격에 남아있는 주문수량의 합계
        int orderCount // 해당 가격에 대기 중인 활성 주문건수
) {

    // 호가와 집계값이 모두 유효한 양수인지 검사한다.
    public OrderBookLevel {
        Objects.requireNonNull(price, "order book price must not be null");
        if (price.amount().signum() <= 0) {
            throw new IllegalArgumentException("order book price must be positive");
        }
        if (totalQuantity <= 0) {
            throw new IllegalArgumentException("total quantity must be positive");
        }
        if (orderCount <= 0) {
            throw new IllegalArgumentException("order count must be positive");
        }
    }
}
