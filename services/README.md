# Institution Services

각 금융기관 역할은 독립 실행 서비스와 독립 데이터 저장소로 구현한다.

- `bank-service`: Bank A/B 공통 코드. 기관별 인스턴스와 DB를 분리한다.
- `broker-service`: Broker A/B 공통 코드. 기관별 인스턴스와 DB를 분리한다.
- `dashboard-service`: Frontend용 BFF. 기관별 조회 API를 조합하고 화면 전용 응답을 제공한다.
- `kftc-service`: 지급지시 중계, 청산주기, 순포지션을 관리한다.
- `bok-service`: 기관 당좌예금과 최종 자금결제를 관리한다.
- `krx-service`: 주문 매칭, 거래 확정, CCP 청산, 결제의무를 관리한다.
- `ksd-service`: 참가자 증권계좌, 증권 보유와 증권결제를 관리한다.

서비스는 다른 서비스의 데이터베이스를 직접 조회하지 않으며 API 또는 메시지 계약으로 통신한다.

기관 서비스는 `libs/common`의 기관 중립적인 값 객체를 사용할 수 있다. 현재 공유 범위는
`InstitutionId`, `CurrencyCode`, `Money`이며, 기관별 Entity, Repository와 업무 규칙은 공유하지 않는다.

`dashboard-service`도 기관 DB에 직접 접근하지 않는다. 기관별 Admin Query API와 Reconciliation API를 호출하며 금융 원장을 수정하거나 Bank-KFTC-BOK 업무 흐름을 대신 오케스트레이션하지 않는다.

## Independent JVM processes

각 디렉터리는 루트 Gradle build에 등록된 독립 Spring Boot subproject다. 다음 태스크는 각각 별도 JVM 프로세스를 실행한다.

```shell
./gradlew :services:bank-service:bootRun
./gradlew :services:broker-service:bootRun
./gradlew :services:dashboard-service:bootRun
./gradlew :services:kftc-service:bootRun
./gradlew :services:bok-service:bootRun
./gradlew :services:krx-service:bootRun
./gradlew :services:ksd-service:bootRun
```

Bank A/B는 같은 애플리케이션을 서로 다른 설정으로 별도 실행한다.

```shell
SERVER_PORT=8081 INSTITUTION_ID=BANK_A ./gradlew :services:bank-service:bootRun
SERVER_PORT=8082 INSTITUTION_ID=BANK_B ./gradlew :services:bank-service:bootRun
```

Broker A/B도 같은 방식으로 포트와 `INSTITUTION_ID`를 분리한다. 개발 중에는 각 명령을 별도 터미널 또는 IntelliJ Run Configuration에서 실행하고, 전체 환경은 이후 Docker Compose로 구성한다.
