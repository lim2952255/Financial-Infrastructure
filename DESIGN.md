# Design

## Source of truth

- Status: Active
- Date: 2026-10-12
- Product surfaces: React web frontend, dashboard-service BFF, institution status views
- Evidence reviewed: `frontend/README.md`, `services/README.md`, current Bank/KFTC/BOK APIs, local frontend Docker and Nginx configuration, 사용자가 제공한 이연차액결제시스템 프로세스 참고 이미지

## Brand

- Personality: 신뢰할 수 있고 차분한 금융 인프라 운영 콘솔
- Trust signals: 기관별 책임 분리, 실제 상태와 잔액, 요청 식별자, 처리 시각, 원장 근거를 함께 표시한다.
- Avoid: 고객용 인터넷뱅킹처럼 보이는 화면, 과도한 핀테크 그라데이션, 장식 목적의 차트, 실제 상태가 아닌 가짜 진행률

## Product goals

- 은행 간 이체가 BANK, KFTC, BOK를 거쳐 처리되는 과정을 한 화면에서 이해시킨다.
- Bank A/B/C의 은행 내 이체와 은행 간 이체를 같은 입력 화면에서 비교할 수 있게 한다.
- 사용자가 이체를 실행하고 지급지시, 청산, 결제 결과와 기관별 잔액 변화를 확인할 수 있게 한다.
- 빠른 동기 API 처리와 별개로 은행 간 6단계, 청산·결제 3단계를 단계당 약 2.4초의 교육용 속도로 시각화한다.
- 동일 요청 재전송을 통해 멱등성을 직접 확인할 수 있게 한다.
- Bank A/B/C를 항상 같은 위치에 표시해 이체마다 기관 카드가 교체되는 오해를 없앤다.
- 동일 이체·결제요청 재전송에서는 일반 자금 이동을 다시 재생하지 않고 `동일 요청 감지 → 기존 결과 조회 → 중복 반영 차단`을 별도 흐름으로 보여준다.
- 이후 증권결제와 DLT 결제 구조를 같은 시각 언어로 비교할 수 있는 기반을 만든다.

Non-goals:

- 초기 버전에서 고객 로그인, 실제 인터넷뱅킹, 운영자 권한 시스템을 구현하지 않는다.
- Frontend나 dashboard-service가 기관 데이터베이스에 직접 접근하거나 금융 업무를 대신 처리하지 않는다.

Success signals:

- 처음 보는 사용자가 이체, 청산, 결제의 차이를 화면만 보고 설명할 수 있다.
- 한 번의 시나리오에서 고객예금 변화와 중앙은행 당좌예금 변화를 모두 추적할 수 있다.
- 동일 요청 재전송 후 잔액과 원장이 중복 반영되지 않았음을 확인할 수 있다.

## Personas and jobs

- 금융 IT 면접관·리뷰어: 짧은 시간 안에 시스템 경계와 금융 흐름을 확인한다.
- 개발자·학습자: 지급지시, 청산, 결제가 코드와 데이터에서 어떻게 연결되는지 실험한다.
- 프로젝트 개발자: 기관별 상태와 실패 지점을 관찰하고 대사한다.

## Information architecture

MVP는 여러 페이지보다 하나의 `Infrastructure View`를 중심으로 구성한다.

- `/`: 기관 흐름도, 이체 입력, 현재 처리상태, 청산·결제 실행, 핵심 잔액
- `/reconciliation`: 기관별 거래·원장·결제 결과 대사
- `/scenarios`: 실패·재시도 실험. MVP 이후 추가
- `/dlt-comparison`: 전통 결제망과 DLT 결제망 비교. DLT 구현 이후 추가

MVP 기본 사용 흐름:

1. BANK_A/B/C의 준비상태와 초기 잔액을 확인한다.
2. 송신계좌, 수신은행·계좌, 금액을 선택하고 `이체 실행`을 누른다.
3. 기관 노드와 타임라인에서 수신은행 입금 완료까지 확인한다.
4. 필요하면 반대 방향 이체를 추가해 순액청산 효과를 만든다.
5. `청산 및 결제 실행`을 누르고 기관별 순포지션과 BOK 당좌예금 변화를 확인한다.
6. `동일 요청 재전송`으로 멱등성을 확인한다.

## Design principles

- Flow first: 테이블보다 기관 흐름도를 먼저 보여준다.
- State, not decoration: 색과 움직임은 실제 처리상태를 표현할 때만 사용한다.
- Process, not latency: 실제 API 지연을 진행률로 오해하지 않도록 완료된 업무 흐름을 일정한 교육용 재생 속도로 보여준다.
- Progressive detail: 기본 화면은 핵심 결과만 보여주고 원장·식별자는 drawer에서 확인한다.
- Explicit money: 모든 단계에서 금액, 이전 잔액, 이후 잔액을 명시한다.
- Institution boundaries: BANK, KFTC, BOK의 책임을 같은 카드 안에 섞지 않는다.

## Visual language

- 상태색: 대기 회색, 처리 중 파란색, 성공 초록색, 실패 빨간색
- 기관색은 상태색과 충돌하지 않는 중립색을 기본으로 사용한다.
- 본문은 최소 14px, 숫자와 식별자는 고정폭 글꼴을 사용할 수 있다.
- 카드 모서리와 그림자는 절제하고, 기관 간 연결선과 상태 변화가 시각적 중심이 되게 한다.
- 업무 메시지 패킷은 현재 처리 중인 연결 구간에서만 이동한다.
- BOK는 KFTC 위에 두고, Bank A/B/C는 KFTC 아래의 고정 위치에 항상 함께 표시한다.
- 은행 간 이체는 `송신은행 → KFTC → 수신은행` 경로만 강조하고, 은행 내 이체는 해당 은행 위의 self-loop로 표현한다.
- 결제 완료 통보는 KFTC에서 해당 청산주기의 실제 참여은행으로만 분기한다.
- 이체 방향의 실선과 성공 응답의 역방향 흐름을 라벨과 화살표 방향으로 구분한다.
- BANK↔KFTC와 KFTC↔BOK 연결은 같은 SVG 선·화살표·파란 패킷 문법을 사용해 기관에 따라 표현이 달라지지 않게 한다.
- 이동 패킷에는 `지급지시`, `입금지시`, `입금완료 응답`, `이체완료 정보`, `결제요청`, `결제 성공결과`, `결제완료 통보`처럼 실제 메시지의 의미를 직접 표시한다.
- 금융결제원 카드에는 현재 청산주기에 아직 포함되지 않은 `청산 대기 지급지시` 건수를 표시하고, 결제완료 통보 단계에서 0건으로 전환한다.

## Components

- `SystemHealthBar`: 컨테이너와 기관 API 준비상태
- `TransferComposer`: 송신계좌, 수신기관·계좌, 금액 입력
- `InstitutionNode`: BANK_A/B/C, KFTC, BOK 상태와 핵심 잔액
- `NetworkPath`: BANK·KFTC·BOK 사이의 요청 방향, 화살표와 현재 처리상태
- `NetworkPacket`: 이동 중인 업무 메시지의 종류를 표시하는 라벨 패킷
- `ClearingQueue`: 금융결제원에 누적된 청산 대상 지급지시 수와 처리·초기화 상태
- `ProcessSteps`: 은행 간 6단계, 은행 내 2단계, 청산·결제 3단계의 현재/완료 상태
- `BankNetwork`: Bank A/B/C 고정 노드, 동적 이체 방향, 은행 내 self-loop, 청산 참여은행 분기
- `FlowTimeline`: 요청 접수부터 최종 결제까지의 사건 목록
- `PendingInstructionTable`: 아직 청산되지 않은 지급지시
- `NetPositionPanel`: 청산주기의 기관별 순포지션
- `LedgerDrawer`: 거래·원장·배치 식별자 상세
- `IdempotencyReplayButton`: 동일 requestId 재전송 및 중복 미반영 확인
- `IdempotencyGuard`: 동일 이체 requestId와 동일 청산주기 ID의 재요청을 황색 확인 흐름과 초록색 차단 결과로 표시

## Accessibility

- 목표: WCAG 2.1 AA 수준의 대비와 키보드 조작
- 상태를 색만으로 표현하지 않고 아이콘과 텍스트를 함께 사용한다.
- 모든 버튼과 기관 노드는 명확한 focus 상태를 가진다.
- `prefers-reduced-motion`에서는 금액 배지 이동을 단계 전환이나 fade로 대체한다.
- 단계의 의미를 움직임에만 의존하지 않고 번호, 텍스트, 현재 상태로 함께 제공한다.
- 처리상태 변경은 적절한 live region으로 알리되 잦은 polling 결과를 모두 읽지 않는다.

## Responsive behavior

- 1280px 이상 데스크톱을 우선한다.
- 좁은 화면에서는 기관 흐름도를 가로 스크롤하거나 세로 단계형으로 전환한다.
- 상세 원장은 데스크톱 drawer, 작은 화면에서는 전체 화면 panel로 표시한다.
- 모바일에서 복잡한 원장 비교 기능을 완전히 제공하는 것은 MVP 범위가 아니다.

## Interaction states

- Loading: 기관별 skeleton보다 전체 시스템 연결 상태를 먼저 표시한다.
- Empty: `아직 지급지시가 없습니다`와 함께 첫 이체 실행 방법을 제공한다.
- Processing: 현재 처리 중인 기관과 연결 구간만 파란색으로 표시한다.
- Route selection: 세 은행 카드는 항상 유지하고 현재 송신·수신 역할과 경로만 변경한다.
- Settlement notification: 청산주기에 포함된 지급지시의 송·수신기관만 참여 배지와 KFTC 통보선을 활성화한다.
- Playback: API가 먼저 완료되어도 정해진 단계 재생이 끝날 때까지 흐름을 유지하며, 이는 실제 처리 지연이 아닌 시각화임을 표시한다.
- Success: 최종 결과와 변경된 잔액을 함께 보여준다.
- Duplicate: 동일 요청은 오류처럼 보이게 하지 않고, 기존 성공 결과 반환과 잔액·원장·지급지시·결제의 중복 반영 차단을 함께 표시한다.
- Error: 실패 기관, 단계, 오류 메시지와 재시도 가능 여부를 표시한다.
- Offline: dashboard-service 또는 기관 서비스 중 어느 연결이 실패했는지 구분한다.
- Disabled: 선행 단계가 끝나기 전에는 청산·결제 버튼을 비활성화하고 이유를 표시한다.

## Content voice

- 금융 용어를 사용하되 짧은 도움말로 뜻을 설명한다.
- `보내기`보다 `이체 실행`, `정산`보다 도메인 용어인 `청산`과 `결제`를 구분한다.
- 상태명은 코드 Enum을 그대로 노출하기보다 한국어 설명과 함께 표시한다.
- 실패 메시지는 책임 기관과 실패 단계를 먼저 말한다.

## Implementation constraints

- React와 Vite를 사용하되 운영·통합 환경에서는 Vite 서버를 실행하지 않는다.
- Docker build의 Node 단계에서 `vite build`를 실행하고 Nginx가 `dist`를 제공한다.
- Browser는 상대경로 `/api/...`로 dashboard-service만 호출한다.
- Nginx가 `/api`와 이후 추가될 `/events`를 dashboard-service로 reverse proxy한다.
- dashboard-service는 기관별 조회 API를 조합하지만 기관 DB를 직접 읽거나 Bank-KFTC-BOK 흐름을 대신 오케스트레이션하지 않는다.
- 현재 동기 처리 구조에서는 내부 중간 상태가 관찰되지 않을 수 있으므로 UI가 임의의 가짜 진행상태를 만들지 않는다.
- 단계 재생은 서버의 실시간 진행률이 아니라 완료된 동기 호출의 업무 순서를 설명하는 시각화로 명확히 표기한다.

## Decisions and follow-ups

- MVP는 하나의 사용자 동작으로 KFTC 청산과 BOK 결제를 연속 실행한다. 두 단계의 결과는 순포지션과 결제 상태로 구분해 표시한다.
- 초기 데이터는 Docker Compose가 기관별 DB에 멱등하게 생성하는 고정 데모 seed를 사용한다.
- 현재 관찰 방식은 2초 polling이다. 동기 HTTP 처리 중간상태를 더 세밀하게 보여줄 필요가 생기면 SSE 기반 관찰 이벤트를 추가한다.
- 데이터 초기화는 화면에 노출하지 않는다. 로컬 운영자가 `docker compose down -v`를 명시적으로 실행할 때만 수행한다.

## Open questions

- [ ] 실제 서버 확정 시각에 맞춘 단계 이벤트가 필요한가? Owner: Product. Impact: 필요하면 SSE 또는 기관 이벤트 저장소가 추가된다.
- [ ] 교육용 단계 재생 속도를 화면에서 사용자가 직접 조절해야 하는가? Owner: Product. Impact: 접근성 설정과 재생 컨트롤 범위가 달라진다.
