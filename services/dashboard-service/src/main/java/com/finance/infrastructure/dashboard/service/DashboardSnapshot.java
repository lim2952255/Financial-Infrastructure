package com.finance.infrastructure.dashboard.service;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;

/**
 * 다섯 기관의 모니터링 결과를 기관별 성공 또는 실패 상태와 함께 묶은 통합 응답이다.
 * 한 기관의 조회가 실패해도 해당 항목만 unavailable로 표현하고 나머지 결과는 유지한다.
 */
public record DashboardSnapshot(
        // 다섯 기관의 조회를 마치고 통합 응답을 생성한 시각
        Instant generatedAt,
        // 기관 ID를 키로 사용해 각 기관의 가용성과 응답을 독립적으로 저장한다.
        Map<String, InstitutionSnapshot> institutions
) {
    public record InstitutionSnapshot(
            // true면 data가 있고, false면 error에 해당 기관의 실패 원인이 있다.
            boolean available,
            JsonNode data,
            String error
    ) {
        static InstitutionSnapshot available(JsonNode data) {
            // 정상 기관은 원본 모니터링 JSON을 유지하고 오류는 비운다.
            return new InstitutionSnapshot(true, data, null);
        }

        static InstitutionSnapshot unavailable(String error) {
            // 실패 기관은 데이터를 비우고 오류만 기록해 다른 기관 결과와 함께 반환한다.
            return new InstitutionSnapshot(false, null, error);
        }
    }
}
