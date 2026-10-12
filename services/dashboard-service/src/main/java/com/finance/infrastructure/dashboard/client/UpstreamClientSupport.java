package com.finance.infrastructure.dashboard.client;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Dashboard가 Bank, KFTC 같은 상위 기관을 호출할 때 발생한 오류를 ResponseStatusException으로 통일한다.
 *
 * 이 클래스는 오류를 무시하거나 장애를 격리하지 않고 변환한 예외를 다시 던진다.
 */
abstract class UpstreamClientSupport {

    protected <T> T call(String institution, UpstreamCall<T> call) {
        try {
            // 각 클라이언트가 전달한 실제 RestClient 요청을 실행한다.
            T response = call.execute();
            if (response == null) {
                // 2xx 응답이어도 본문이 없다면 정상적인 기관 응답으로 사용할 수 없다.
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        institution + " returned an empty response"
                );
            }
            return response;
        } catch (RestClientResponseException exception) {
            // 기관이 HTTP 오류를 반환한 경우 상태 코드를 유지해 호출자가 원인을 구분할 수 있게 한다.
            throw new ResponseStatusException(
                    exception.getStatusCode(),
                    institution + " request failed",
                    exception
            );
        } catch (RestClientException exception) {
            // 연결 거부, 통신 실패 등 HTTP 응답조차 받지 못한 경우에는 상위 기관 장애로 표현한다.
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    institution + " is unavailable",
                    exception
            );
        }
    }

    @FunctionalInterface
    protected interface UpstreamCall<T> {
        // BankClient, KftcClient, MonitoringClient가 실제 HTTP 호출을 람다로 전달한다.
        T execute();
    }
}
