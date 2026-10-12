# Financial Infrastructure Lab Frontend

Bank A/B/C의 은행 내·은행 간 이체와 KFTC, BOK의 청산·결제 흐름을 한 화면에서 조작하고 관찰하는 React UI다. 빠른 API 처리 결과는 은행 간 6단계, 청산·결제 3단계로 재생하며 동일 요청 재전송은 별도의 중복 반영 차단 흐름으로 표시한다. 브라우저는 상대경로 `/api`로 `dashboard-service`만 호출하며 기관 서비스나 데이터베이스에 직접 접근하지 않는다.

## Production image

운영 및 통합 실행에서는 Vite 개발 서버를 사용하지 않는다. Docker의 Node 단계가 정적 자산을 빌드하고 Nginx가 결과물을 제공한다.

```bash
docker build -t finance-infrastructure-frontend ./frontend
```

Nginx는 `/api`와 `/events` 요청을 Docker Compose 서비스 이름 `dashboard:8080`으로 전달한다.

## Local build check

```bash
npm ci
npm run build
```

`npm run dev`는 UI 단독 개발이 필요할 때만 사용한다.
