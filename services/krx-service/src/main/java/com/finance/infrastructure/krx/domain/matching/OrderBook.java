package com.finance.infrastructure.krx.domain.matching;

import com.finance.infrastructure.common.money.Money;
import com.finance.infrastructure.krx.domain.order.ExchangeOrder;
import com.finance.infrastructure.krx.domain.order.OrderSide;
import com.finance.infrastructure.krx.domain.security.ListedSecurity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;

// 한 종목의 매수·매도 주문을 관리하고 가격 우선, 동일 가격에서는 접수 순서 우선으로 매칭하는 호가창
public final class OrderBook {

    // 매수 주문은 높은 가격을 우선하고, 동일 가격이면 먼저 접수된 주문을 우선한다.
    private static final Comparator<ExchangeOrder> BUY_PRIORITY = Comparator
            .comparing((ExchangeOrder order) -> order.limitPrice().amount(), Comparator.reverseOrder()) // 가격의 내림차순으로 정렬
            .thenComparingLong(ExchangeOrder::getReceivedSequence) // 가격이 같으면 먼저 접수된 주문을 우선적으로 정렬
            .thenComparing(ExchangeOrder::getExchangeOrderId);

    // 매도 주문은 낮은 가격을 우선하고, 동일 가격이면 먼저 접수된 주문을 우선한다.
    private static final Comparator<ExchangeOrder> SELL_PRIORITY = Comparator
            .comparing((ExchangeOrder order) -> order.limitPrice().amount()) // 가격의 오름차순으로 정렬
            .thenComparingLong(ExchangeOrder::getReceivedSequence) // 가격이 같으면 먼저 접수된 주문을 우선적으로 정렬
            .thenComparing(ExchangeOrder::getExchangeOrderId);

    // 이 호가창이 관리하는 상장종목
    private final ListedSecurity security;
    // 가격·시간 우선순위로 정렬된 매수 주문
    private final PriorityQueue<ExchangeOrder> buyOrders = new PriorityQueue<>(BUY_PRIORITY);
    // 가격·시간 우선순위로 정렬된 매도 주문
    private final PriorityQueue<ExchangeOrder> sellOrders = new PriorityQueue<>(SELL_PRIORITY);
    // 동일 주문이 호가창에 중복 접수되는 것을 방지하기 위한 거래소 주문번호 집합
    private final Set<String> acceptedOrderIds = new HashSet<>();

    public OrderBook(ListedSecurity security) {
        this.security = Objects.requireNonNull(security, "listed security must not be null");
    }

    public ListedSecurity security() {
        return security;
    }

    // 활성 주문을 호가창에 접수하고 반대 주문과 가격이 교차하면 가능한 수량만큼 즉시 매칭한다.
    public List<Match> submit(ExchangeOrder order) {
        Objects.requireNonNull(order, "exchange order must not be null");
        requireSameSecurity(order); // 새로 들어온 주문이 해당 호가창이 관리하는 상장 종목인지 검사한다.
        if (!order.isActive()) {
            throw new IllegalStateException("only an active order can enter the order book");
        }
        // 중복 거래요청을 여러번 처리하지 않기 위해 방지한다(멱등성 보장)
        if (!acceptedOrderIds.add(order.getExchangeOrderId())) {
            throw new IllegalArgumentException("order is already in the order book: " + order.getExchangeOrderId());
        }

        // 주문방향에 맞는 주문 대기열을 반환한다(매수주문 -> 매수대기열, 매도주문 -> 매도대기열)
        queueFor(order.getSide()).add(order);
        return matchCrossedOrders();
    }

    // 현재 남아있는 매수 주문을 가격별 수량과 주문건수로 집계한다.
    // 이때 가격별 수량과 주문건수는 OrderBookLevel 레코드에 담겨 있다.
    public List<OrderBookLevel> buyLevels() {
        return levels(buyOrders, Comparator.reverseOrder());
    }

    // 현재 남아있는 매도 주문을 가격별 수량과 주문건수로 집계한다.
    // 이때 가격별 수량과 주문건수는 OrderBookLevel 레코드에 담겨 있다.
    public List<OrderBookLevel> sellLevels() {
        return levels(sellOrders, Comparator.naturalOrder());
    }

    // 최우선 매수호가가 최우선 매도호가 이상인 동안 두 주문을 반복해서 체결한다.
    private List<Match> matchCrossedOrders() {
        List<Match> matches = new ArrayList<>(); // 체결된 거래정보를 담는 list
        pruneInactive(buyOrders); // 체결완료 또는 취소등으로 비활성화된 최우선 주문을 매수대기열에서 제거
        pruneInactive(sellOrders); // 체결완료 또는 취소등으로 비활성화된 최우선 주문을 매도대기열에서 제거

        while (!buyOrders.isEmpty() && !sellOrders.isEmpty()) {
            ExchangeOrder bestBuy = buyOrders.peek(); // 최우선 매수호가 주문
            ExchangeOrder bestSell = sellOrders.peek(); // 최우선 매도호가 주문
            // 최우선 매수호가가 최우선 매도호가보다 작으면 거래 체결 불가
            if (bestBuy.limitPrice().amount().compareTo(bestSell.limitPrice().amount()) < 0) {
                break;
            }

            // 최우선 매수호가에 걸려있는 종목수량과 최우선 매도호가에 걸려있는 종목수량중 더 작은 값을 조회한다.
            long quantity = Math.min(bestBuy.remainingQuantity(), bestSell.remainingQuantity());
            Money price = executionPrice(bestBuy, bestSell); // 최우선 매수호가와 최우선 매도호가중 먼저 거래요청이 들어온 주문의 지정가격을 체결가격으로 사용한다.
            bestBuy.fill(quantity); // 이번 거래에 체결된 수량을 매수주문에 누적
            bestSell.fill(quantity); // 이번 거래에 체결된 수량을 매도주문에 누적
            matches.add(new Match(bestBuy, bestSell, price, quantity)); // 현재 매칭결과를 matches에 추가한다.

            pruneInactive(buyOrders); // 체결완료 또는 취소등으로 비활성화된 최우선 주문을 매수대기열에서 제거
            pruneInactive(sellOrders); // 체결완료 또는 취소등으로 비활성화된 최우선 주문을 매도대기열에서 제거
        }
        return List.copyOf(matches);
    }

    // 가격이 교차한 두 주문 중 먼저 접수된 주문의 지정가격을 체결가격으로 사용한다.
    private Money executionPrice(ExchangeOrder buyOrder, ExchangeOrder sellOrder) {
        return buyOrder.getReceivedSequence() < sellOrder.getReceivedSequence()
                ? buyOrder.limitPrice()
                : sellOrder.limitPrice();
    }

    // 활성 주문을 동일 가격 단위로 묶어 호가창 조회 값으로 변환한다.
    private List<OrderBookLevel> levels(
            PriorityQueue<ExchangeOrder> orders,
            Comparator<BigDecimal> priceComparator
    ) {
        Map<BigDecimal, MutableLevel> levels = new TreeMap<>(priceComparator);
        for (ExchangeOrder order : orders) {
            if (!order.isActive()) {
                continue;
            }
            levels.computeIfAbsent(order.limitPrice().amount(), ignored -> new MutableLevel())
                    .add(order.remainingQuantity());
        }

        return levels.entrySet().stream()
                .map(entry -> new OrderBookLevel(
                        new Money(entry.getKey()),
                        entry.getValue().totalQuantity,
                        entry.getValue().orderCount
                ))
                .toList();
    }

    // 주문 방향에 맞는 매수 또는 매도 대기열을 반환한다.
    private PriorityQueue<ExchangeOrder> queueFor(OrderSide side) {
        return side == OrderSide.BUY ? buyOrders : sellOrders;
    }

    // 접수하려는 주문이 이 호가창과 동일한 종목의 주문인지 검사한다.
    private void requireSameSecurity(ExchangeOrder order) {
        if (!security.getSecurityCode().equals(order.getSecurity().getSecurityCode())) {
            throw new IllegalArgumentException("order belongs to a different security");
        }
    }

    // 체결완료 또는 취소 등으로 비활성화된 최우선 주문을 대기열에서 제거한다.
    private static void pruneInactive(PriorityQueue<ExchangeOrder> orders) {
        while (!orders.isEmpty() && !orders.peek().isActive()) {
            orders.remove();
        }
    }

    // 동일 가격의 잔여수량과 활성 주문건수를 누적하기 위한 내부 집계 객체
    private static final class MutableLevel {
        private long totalQuantity;
        private int orderCount;

        private void add(long quantity) {
            totalQuantity = Math.addExact(totalQuantity, quantity);
            orderCount = Math.addExact(orderCount, 1);
        }
    }
}
