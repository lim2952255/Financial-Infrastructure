# Financial Infrastructure Lab

은행 내 이체와 은행 간 이체가 Bank, KFTC, BOK를 거쳐 입금·청산·최종 결제되는 과정을 직접 실행하고 관찰하는 프로젝트다. 각 기관 애플리케이션과 데이터베이스는 독립 컨테이너로 실행되며, 브라우저는 `dashboard-service`를 통해서만 기관 API를 호출한다.

## 실행

Docker Desktop을 실행한 뒤 프로젝트 루트에서 다음 명령을 실행한다.

```bash
cp .env.example .env
docker compose up --build -d
```

모든 컨테이너가 준비되면 [http://localhost:5173](http://localhost:5173)을 연다.

```bash
docker compose ps
```

화면에서 다음 순서로 흐름을 확인할 수 있다.

1. `계좌 이체`에서 Bank A/B/C의 송신·수신 계좌를 선택한다. 같은 은행의 다른 계좌를 선택하면 은행 내 이체가 실행된다.
2. 은행 간 이체의 6단계 지급지시 흐름 또는 은행 내 원자적 이체 흐름과 고객예금 잔액 변화를 확인한다.
3. `동일 이체요청 재전송`으로 requestId 멱등성과 중복 반영 차단 애니메이션을 확인한다.
4. `청산 및 결제 실행`으로 KFTC 순포지션과 BOK 당좌예금 변화를 확인한다.
5. `동일 결제요청 재전송`으로 같은 청산주기 ID가 기존 결과를 반환하고 결제를 중복 반영하지 않는지 확인한다.

컨테이너만 중지하고 데이터는 보존하려면 다음 명령을 사용한다.

```bash
docker compose down
```

데모 데이터를 완전히 초기화하려면 볼륨까지 제거한 뒤 다시 실행한다. 이 명령은 현재 Compose 환경의 다섯 MySQL 데이터를 삭제한다.

```bash
docker compose down -v
docker compose up --build -d
```

## 실행 구조

- `frontend`: React 정적 빌드를 Nginx로 제공하고 `/api`를 Dashboard로 프록시한다.
- `dashboard-service`: 프론트용 BFF로 기관별 상태를 조합하고 사용자 동작을 해당 기관 API로 전달한다.
- `bank-a`, `bank-b`, `bank-c`: 동일한 `bank-service` 이미지에 서로 다른 기관 설정과 MySQL을 적용한다. 각 은행은 은행 내 이체 시연을 위한 두 개의 고객계좌를 가진다.
- `kftc-service`: 지급지시 중계, 청산주기, 순포지션, BOK 결제요청을 관리한다.
- `bok-service`: 기관별 당좌예금, 결제배치, 결제원장을 관리한다.

기관 서비스는 서로의 데이터베이스를 직접 조회하지 않고 HTTP API로만 통신한다. 호스트에는 프론트의 `5173` 포트만 노출된다.

## 주요 조회 API

- Bank: `GET /api/monitoring`
- KFTC: `GET /api/monitoring`
- BOK: `GET /api/monitoring`
- Dashboard 통합 상태: `GET /api/dashboard/snapshot`

로컬 호스트에서는 Nginx를 통해 `http://localhost:5173/api/dashboard/snapshot`으로 통합 상태를 확인할 수 있다.

## 검증

```bash
./gradlew :services:bank-service:test \
  :services:kftc-service:test \
  :services:bok-service:test \
  :services:dashboard-service:test

cd frontend
npm ci
npm run build
```
