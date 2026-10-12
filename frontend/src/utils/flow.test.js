import test from 'node:test'
import assert from 'node:assert/strict'
import { buildIdempotencyFlow, buildSettlementFlow, buildTransferFlow, receiverCandidates } from './flow.js'

// 사용자에게 보여주는 단계 수와 순서가 업무 요구사항에서 벗어나지 않도록 고정한다.
test('은행 간 이체는 요청한 여섯 단계를 순서대로 제공한다', () => {
  const flow = buildTransferFlow('BANK_A', 'BANK_C')

  assert.equal(flow.kind, 'interbank-transfer')
  assert.deepEqual(flow.steps.map((step) => step.id), [1, 2, 3, 4, 5, 6])
  assert.deepEqual(flow.steps.map((step) => step.focus), [
    'sender',
    'sender-to-kftc',
    'kftc-to-receiver',
    'receiver-to-kftc',
    'kftc-to-sender',
    'sender-complete',
  ])
})

test('은행 내 이체는 KFTC와 BOK를 거치지 않는다', () => {
  const flow = buildTransferFlow('BANK_B', 'BANK_B')

  assert.equal(flow.kind, 'internal-transfer')
  assert.equal(flow.steps.length, 2)
  assert.equal(flow.steps.some((step) => step.focus.includes('kftc')), false)
})

test('청산 및 결제는 세 단계 응답 흐름을 제공한다', () => {
  const flow = buildSettlementFlow()

  assert.deepEqual(flow.steps.map((step) => step.focus), [
    'kftc-to-bok',
    'bok-to-kftc',
    'kftc-to-banks',
  ])
})

test('순포지션이 0원이면 BOK 결제 단계를 생략한다', () => {
  const flow = buildSettlementFlow(false)

  assert.equal(flow.requiresBok, false)
  assert.deepEqual(flow.steps.map((step) => step.focus), ['kftc-zero-net', 'kftc-to-banks'])
})

test('수신 후보는 송신계좌 자신만 제외하고 동일 은행의 다른 계좌를 포함한다', () => {
  const accounts = [
    { institutionId: 'BANK_A', accountNumber: 'A-001' },
    { institutionId: 'BANK_A', accountNumber: 'A-002' },
    { institutionId: 'BANK_B', accountNumber: 'B-001' },
  ]

  assert.deepEqual(receiverCandidates(accounts, accounts[0]), [accounts[1], accounts[2]])
})

test('동일 요청 재전송은 자금 이동 대신 중복 반영 차단을 표시한다', () => {
  const transferReplay = buildIdempotencyFlow('transfer')
  const settlementReplay = buildIdempotencyFlow('settlement')

  assert.equal(transferReplay.kind, 'idempotency-replay')
  assert.equal(settlementReplay.label, '동일 결제요청 재전송')
  assert.equal(transferReplay.steps.at(-1).focus, 'duplicate-blocked')
  assert.match(transferReplay.steps.at(-1).title, /중복 반영 차단/)
})
