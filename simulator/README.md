# Bank Reconciliation Simulator

FinMate의 계정·원장 모델에서 필요한 부분만 가져온 은행 간 송금 시뮬레이터다. 동일한 Spring Boot 이미지를 `BANK_A`, `BANK_B`로 각각 실행하고, 각 인스턴스는 독립된 MySQL만 사용한다.

## 구성

- `bank-a`: `mysql-a`만 접근, 기본 계좌 `A-1001`과 잔액 `2,000,000`
- `bank-b`: `mysql-b`만 접근, 기본 계좌 `B-2001`과 잔액 `500,000`
- 로컬 포트: Bank A `8081`, Bank B `8082`

## 프로젝트 구조

FinMate처럼 역할별 최상위 패키지로 분리하되, 시뮬레이션에 필요하지 않은 인증·사용자·투자 도메인은 제외했다.

```text
bank/
├── controller/   # 계좌·원장·송금 HTTP API와 예외 응답
├── service/      # 계좌 업무, 로컬 원장 반영, 은행 간 송금 흐름
├── domain/       # Account, LedgerEntry, OutgoingTransfer 엔티티와 상태 값
├── repository/   # 엔티티별 Spring Data JPA 저장소
├── dto/          # API 요청·응답 모델
├── config/       # 은행별 환경설정, HTTP 클라이언트, 데모 데이터
└── exception/    # 계좌·잔액·중복·장애 시뮬레이션 예외
```

송금 흐름을 이해하려면 `TransferController` → `OutgoingTransferService` → `LedgerPostingService` → `InternalTransferController` 순서로 읽는다.

## 실행

먼저 예시 파일을 복사한 뒤 DB 계정, 비밀번호, 은행 ID, 데모 계좌를 원하는 값으로 변경한다.

```bash
cp .env.example .env
```

그다음 저장소 루트에서 실행한다.

```bash
docker compose up --build -d
docker compose ps
```

초기 계좌 확인:

```bash
curl http://localhost:8081/api/accounts/A-1001
curl http://localhost:8082/api/accounts/B-2001
```

## 정상 송금

Bank A의 출금 로컬 트랜잭션이 먼저 커밋되고, 이후 Bank B 입금 API를 호출한다.

```bash
curl -X POST http://localhost:8081/api/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "transactionId": "TX-001",
    "sourceAccountNumber": "A-1001",
    "destinationBankId": "BANK_B",
    "destinationAccountNumber": "B-2001",
    "amount": 100000,
    "simulateDestinationFailure": false
  }'
```

양쪽 원장 확인:

```bash
curl 'http://localhost:8081/api/ledger?transactionId=TX-001'
curl 'http://localhost:8082/api/ledger?transactionId=TX-001'
```

## 부분 성공 시뮬레이션

아래 요청은 Bank A 출금 이후 Bank B가 의도적으로 `503`을 반환하게 한다. 결과적으로 Bank A에는 `DEBIT` 원장이 남지만 Bank B에는 `CREDIT` 원장이 없다.

```bash
curl -X POST http://localhost:8081/api/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "transactionId": "TX-FAIL-001",
    "sourceAccountNumber": "A-1001",
    "destinationBankId": "BANK_B",
    "destinationAccountNumber": "B-2001",
    "amount": 100000,
    "simulateDestinationFailure": true
  }'
```

불일치 확인:

```bash
curl http://localhost:8081/api/transfers/TX-FAIL-001
curl 'http://localhost:8081/api/ledger?transactionId=TX-FAIL-001'
curl 'http://localhost:8082/api/ledger?transactionId=TX-FAIL-001'
```

## 원장 불일치 직접 주입

동일한 거래 ID에 서로 다른 금액을 기록해 대사 입력을 만들 수 있다.

```bash
curl -X POST http://localhost:8081/api/ledger/debits \
  -H 'Content-Type: application/json' \
  -d '{
    "transactionId": "TX-MISMATCH-001",
    "accountNumber": "A-1001",
    "amount": 100000,
    "counterpartyBankId": "BANK_B",
    "counterpartyAccountNumber": "B-2001"
  }'

curl -X POST http://localhost:8082/api/ledger/credits \
  -H 'Content-Type: application/json' \
  -d '{
    "transactionId": "TX-MISMATCH-001",
    "accountNumber": "B-2001",
    "amount": 90000,
    "counterpartyBankId": "BANK_A",
    "counterpartyAccountNumber": "A-1001"
  }'
```

현재 단계에서는 두 조회 결과를 직접 비교한다. 다음 단계에서 별도의 `reconciliation-service`가 이를 자동 비교하고 차이 보고서를 만들게 된다.

## 초기화

시뮬레이션 데이터를 모두 삭제하고 다시 시작한다.

```bash
docker compose down -v
docker compose up --build -d
```

`down -v`는 두 MySQL의 시뮬레이션 데이터 볼륨을 삭제한다.
