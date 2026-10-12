// 실제 API 응답과 별개로 각 업무 메시지를 눈으로 읽을 수 있게 재생하는 교육용 시간이다.
export const FLOW_STEP_DURATION_MS = 2400

// 실제 처리 속도와 별개로 사용자가 업무 순서를 읽을 수 있게 재생할 단계 정의다.
const INTERBANK_STEPS = [
  { id: 1, focus: 'sender', title: '송신은행 예금 차감', description: '송신계좌에서 이체 금액을 차감하고 출금 원장을 기록합니다.' },
  { id: 2, focus: 'sender-to-kftc', title: 'KFTC로 지급지시', description: '송신은행이 금융결제원에 은행 간 지급지시를 전달합니다.' },
  { id: 3, focus: 'kftc-to-receiver', title: '수신은행에 입금지시', description: 'KFTC가 지급지시를 확인하고 수신은행으로 입금지시를 보냅니다.' },
  { id: 4, focus: 'receiver-to-kftc', title: '예금 증액 · 성공결과 전달', description: '수신은행이 수신계좌를 증액하고 KFTC에 성공 결과를 응답합니다.' },
  { id: 5, focus: 'kftc-to-sender', title: '지급지시 확정 · 완료정보 전달', description: 'KFTC가 지급지시를 확정하고 송신은행에 완료 정보를 반환합니다.' },
  { id: 6, focus: 'sender-complete', title: '송신은행 이체 성공', description: '송신은행이 성공신호를 받고 거래를 완료 상태로 확정합니다.' },
]

const INTERNAL_STEPS = [
  { id: 1, focus: 'sender', title: '송신계좌 예금 차감', description: '같은 은행의 송신계좌에서 이체 금액을 차감합니다.' },
  { id: 2, focus: 'internal-receiver', title: '수신계좌 예금 증액', description: '같은 데이터베이스 트랜잭션에서 수신계좌를 증액하고 거래를 완료합니다.' },
]

const SETTLEMENT_STEPS = [
  { id: 1, focus: 'kftc-to-bok', title: 'KFTC 청산 · BOK 결제요청', description: 'KFTC가 순포지션을 계산하고 한국은행에 최종 결제를 요청합니다.' },
  { id: 2, focus: 'bok-to-kftc', title: 'BOK 결제 · 성공결과 전달', description: '한국은행이 당좌예금 결제를 수행하고 KFTC에 성공 결과를 반환합니다.' },
  { id: 3, focus: 'kftc-to-banks', title: '참여은행에 결제 성공응답', description: 'KFTC가 이번 청산주기에 참여한 은행에만 최종 결제 완료를 통보합니다.' },
]

const ZERO_NET_SETTLEMENT_STEPS = [
  { id: 1, focus: 'kftc-zero-net', title: 'KFTC 청산 · 순포지션 0원 확인', description: '기관별 순포지션 합계가 모두 0원이어서 중앙은행 자금 이동이 필요하지 않습니다.' },
  { id: 2, focus: 'kftc-to-banks', title: '참여은행에 청산 완료응답', description: 'BOK 결제를 생략하고 이번 청산주기에 참여한 은행에 완료를 통보합니다.' },
]

const IDEMPOTENCY_STEPS = [
  { id: 1, focus: 'duplicate-received', title: '동일 요청 재수신', description: '이전에 사용한 requestId 또는 청산주기 ID가 다시 전달됩니다.' },
  { id: 2, focus: 'duplicate-check', title: '기존 처리결과 조회', description: '기관이 저장된 거래·지급지시·결제 결과와 요청 내용을 대조합니다.' },
  { id: 3, focus: 'duplicate-blocked', title: '중복 반영 차단', description: '새로운 자금 이동 없이 기존 성공 결과를 그대로 반환합니다.' },
]

export function buildTransferFlow(senderInstitutionId, receiverInstitutionId) {
  const internal = senderInstitutionId === receiverInstitutionId
  return {
    kind: internal ? 'internal-transfer' : 'interbank-transfer',
    label: internal ? '은행 내 이체' : '은행 간 이체',
    description: internal
      ? '동일 은행 안에서 원자적으로 처리되며 KFTC와 BOK를 거치지 않습니다.'
      : '실제 API 결과를 바탕으로 은행 간 업무 순서를 천천히 재생합니다.',
    steps: internal ? INTERNAL_STEPS : INTERBANK_STEPS,
  }
}

export function buildSettlementFlow(requiresBok = true) {
  return {
    kind: 'settlement',
    requiresBok,
    label: requiresBok ? '청산 및 결제' : '청산 완료 · BOK 결제 생략',
    description: requiresBok
      ? 'KFTC 청산부터 BOK 결제, 참여은행 통보까지의 업무 순서를 재생합니다.'
      : '순포지션이 0원이어서 BOK 자금 이동 없이 참여은행 통보까지 재생합니다.',
    steps: requiresBok ? SETTLEMENT_STEPS : ZERO_NET_SETTLEMENT_STEPS,
  }
}

export function buildIdempotencyFlow(target) {
  // 동일 요청은 실패로 꾸미지 않고 기존 성공 결과 재사용과 중복 반영 차단으로 설명한다.
  const settlement = target === 'settlement'
  return {
    kind: 'idempotency-replay',
    target,
    label: settlement ? '동일 결제요청 재전송' : '동일 이체요청 재전송',
    description: '동일 요청을 오류로 끝내는 대신 기존 결과를 반환하고 중복 원장 반영을 차단합니다.',
    steps: IDEMPOTENCY_STEPS.map((step) => ({
      ...step,
      description: step.id === 1
        ? settlement
          ? '이미 처리한 청산주기 ID가 KFTC에 다시 전달됩니다.'
          : '이미 처리한 이체 requestId가 송신은행에 다시 전달됩니다.'
        : step.description,
    })),
  }
}

export function accountKey(account) {
  return `${account?.institutionId ?? ''}:${account?.accountNumber ?? ''}`
}

export function receiverCandidates(accounts, sender) {
  if (!sender) return []
  const senderKey = accountKey(sender)
  return accounts.filter((account) => accountKey(account) !== senderKey)
}
