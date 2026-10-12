export const BANK_IDS = ['BANK_A', 'BANK_B', 'BANK_C']
const INSTITUTION_IDS = [...BANK_IDS, 'KFTC', 'BOK']

// 서로 다른 기관 모니터링 응답을 화면에서 쓰는 하나의 안정된 모델로 정규화한다.
export function emptySnapshot() {
  return {
    updatedAt: null,
    institutions: INSTITUTION_IDS.map((id) => ({ id, name: institutionLabel(id), status: 'UNKNOWN', error: null })),
    accounts: [],
    ledgerEntries: [],
    instructions: [],
    clearing: {},
    clearingCycles: [],
    settlementSubmissions: [],
    positions: [],
    settlementAccounts: [],
    settlementBatches: [],
    settlementLedgerEntries: [],
    transactions: [],
  }
}

function arrayOf(value, fieldName) {
  if (value === undefined) return []
  if (!Array.isArray(value)) throw new Error(`Dashboard 응답의 ${fieldName} 형식이 올바르지 않습니다.`)
  return value
}

function institutionSnapshot(id, wrapper) {
  if (!wrapper || typeof wrapper.available !== 'boolean') {
    throw new Error(`Dashboard 응답에 ${id} 기관 상태가 없습니다.`)
  }
  if (wrapper.available && (!wrapper.data || typeof wrapper.data !== 'object')) {
    throw new Error(`Dashboard 응답의 ${id} 기관 데이터가 없습니다.`)
  }
  return {
    id,
    name: institutionLabel(id),
    status: wrapper.available ? 'UP' : 'DOWN',
    error: wrapper.error ?? null,
  }
}

export function normalizeSnapshot(raw = {}) {
  if (!raw.institutions || typeof raw.institutions !== 'object') {
    throw new Error('Dashboard 응답에 기관 상태가 없습니다.')
  }

  const banks = BANK_IDS.map((id) => ({ id, data: raw.institutions[id]?.data ?? {} }))
  const kftc = raw.institutions.KFTC?.data ?? {}
  const bok = raw.institutions.BOK?.data ?? {}

  const accounts = banks.flatMap(({ id, data }) =>
    arrayOf(data.accounts, `${id}.accounts`).map((account) => ({
      ...account,
      institutionId: data.institutionId ?? id,
    })))

  const clearingCycles = arrayOf(kftc.clearingCycles, 'KFTC.clearingCycles')
  const submissions = arrayOf(kftc.settlementSubmissions, 'KFTC.settlementSubmissions')
  const latestCycle = clearingCycles.at(-1) ?? {}
  const latestSubmission = submissions.at(-1)
  const clearing = {
    ...latestCycle,
    submissionId: latestSubmission?.submissionId,
    submissionStatus: latestSubmission?.status,
  }

  const transactions = banks
    .flatMap(({ id, data }) => arrayOf(data.transactions, `${id}.transactions`))
    .sort((left, right) => String(right.createdAt ?? '').localeCompare(String(left.createdAt ?? '')))
  const ledgerEntries = banks.flatMap(({ id, data }) =>
    arrayOf(data.ledgerEntries, `${id}.ledgerEntries`).map((entry) => ({
      ...entry,
      institutionId: id,
    })))

  return {
    updatedAt: raw.generatedAt ?? new Date().toISOString(),
    institutions: INSTITUTION_IDS.map((id) => institutionSnapshot(id, raw.institutions[id])),
    accounts,
    ledgerEntries,
    instructions: arrayOf(kftc.paymentInstructions, 'KFTC.paymentInstructions'),
    clearing,
    clearingCycles,
    settlementSubmissions: submissions,
    positions: arrayOf(latestCycle.positions, 'KFTC.clearingCycles.positions'),
    settlementAccounts: arrayOf(bok.settlementAccounts, 'BOK.settlementAccounts'),
    settlementBatches: arrayOf(bok.batches, 'BOK.batches'),
    settlementLedgerEntries: arrayOf(bok.ledgerEntries, 'BOK.ledgerEntries'),
    transactions,
  }
}

export function institutionLabel(id) {
  const labels = {
    BANK_A: 'Bank A',
    BANK_B: 'Bank B',
    BANK_C: 'Bank C',
    KFTC: '금융결제원',
    BOK: '한국은행',
  }
  return labels[String(id).toUpperCase()] ?? id ?? '기관'
}

export function statusTone(status) {
  const value = String(status ?? '').toUpperCase()
  if (['UP', 'ACTIVE', 'HEALTHY', 'COMPLETED', 'SETTLED', 'SUCCESS', 'RECEIVER_CREDITED'].includes(value)) return 'success'
  if (['PROCESSING', 'FORWARDED', 'RECEIVED', 'SUBMITTED', 'CALCULATED'].includes(value)) return 'processing'
  if (['DOWN', 'FAILED', 'FAILURE', 'REJECTED', 'ERROR', 'OFFLINE'].includes(value)) return 'failure'
  return 'waiting'
}

export function statusLabel(status) {
  const value = String(status ?? 'UNKNOWN').toUpperCase()
  const labels = {
    UP: '정상', ACTIVE: '정상', HEALTHY: '정상', DOWN: '연결 실패', OFFLINE: '연결 실패',
    RECEIVED: '지급지시 접수', FORWARDED: '수신은행 전달', RECEIVER_CREDITED: '수신계좌 입금',
    CLEARED: '청산 완료', CALCULATED: '순포지션 계산', SETTLED: '결제 완료', COMPLETED: '처리 완료',
    SUBMITTED: '결제 요청', PROCESSING: '처리 중', FAILED: '실패', REJECTED: '거절', UNKNOWN: '대기', OPEN: '청산 대기',
  }
  return labels[value] ?? value
}

export function formatMoney(value) {
  const number = Number(value ?? 0)
  return Number.isFinite(number) ? `${new Intl.NumberFormat('ko-KR').format(number)}원` : '-'
}

export function formatTime(value) {
  if (!value) return '-'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? String(value) : new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  }).format(date)
}
