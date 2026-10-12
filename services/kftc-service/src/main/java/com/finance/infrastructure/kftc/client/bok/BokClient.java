package com.finance.infrastructure.kftc.client.bok;

import com.finance.infrastructure.kftc.config.KftcProperties;
import com.finance.infrastructure.kftc.domain.clearing.ClearingCycle;
import com.finance.infrastructure.kftc.domain.clearing.NetPosition;
import com.finance.infrastructure.kftc.domain.settlement.SettlementSubmission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// KFTC의 기관별 순포지션을 BOK 결제 API에 제출한다.
@Component
@RequiredArgsConstructor
public class BokClient {

    private final RestClient.Builder restClientBuilder; // URL을 설정하고, HTTP 요청을 전송할 수 있는 RestClient를 생성
    private final KftcProperties properties; // BOK 기관의 url정보를 저장하고 있는 propertiesx

    // 특정 청산주기의 청산 결과를 BOK에 결제요청을 전송하고, BOK의 결제 결과를 받는 메서드
    public SettlementResponse settle(
            // 청산 결과를 BOK에 제출하고 처리 결과를 관리하는 엔티티
            SettlementSubmission submission,
            // 청산주기를 관리하는 엔티티
            ClearingCycle clearingCycle
    ) {

        // 청산결과에 담겨있는 금융기관별 순포지션 정보들을 PositionRequest 레코드로 생성하여 List로 변환한다.
        List<PositionRequest> positions = clearingCycle.getPositions().stream()
                .map(this::toRequest)
                .toList();

        // BOK에게 청산결과 및 각 금융기관의 순포지션 정보를 전달하고, BOK의 결제결과를 응답받는다.
        SettlementResponse response = restClientBuilder
                .baseUrl(properties.bokBaseUrl())
                .build()
                .post()
                .uri("/api/settlement-batches")
                .body(new SettlementRequest(
                        submission.getSubmissionId(), // submission의 submissionId를 결제 배치 id로 설정한다.
                        clearingCycle.getCycleId(), // 현재 청산주기의 청산주기 id를 등록한다.
                        positions // 각 기관별 순포지션정보를 전송한다.
                ))
                .retrieve() // BOK에 결제요청 정보를 전송하고, BOK로부터 응답을 받을떄까지 대기한다
                .body(SettlementResponse.class); // BOK로부터 받은 응답 데이터(Json 문자열)를 SettlementResponse 객체로 변환한다.

        if (response == null) {
            throw new IllegalStateException("BOK returned an empty settlement response");
        }
        return response;
    }
    // 금융기관별 순포지션 정보를 바탕으로 BOK에 전송할 레코드를 생성한다.
    private PositionRequest toRequest(NetPosition position) {
        return new PositionRequest(
                position.institutionId().value(),
                position.netAmount().amount()
        );
    }

    // KFTC가 BOK에 전송하는 청산정보(결제정보)
    public record SettlementRequest(
            String batchId, // 결제 배치
            String clearingCycleId, // 청산주기
            List<PositionRequest> positions // 각 금융기관별 순포지션
    ) {
    }

    // 특정 금융기관의 순포지션정보를 저장하는 레코드
    public record PositionRequest(String institutionId, BigDecimal netAmount) {
    }

    // 현재 청산 결과가 BOK에서 어떤 결제 배치에 포함되고, 언제 결제되고, 처리상태 응답정보를 저장하는 레코드
    public record SettlementResponse(
            String batchId,
            String status,
            LocalDateTime settledAt
    ) {
    }
}
