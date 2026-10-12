import { useMemo, useState } from 'react'
import { createTransfer, settleClearingCycle } from './api/dashboard.js'
import { useDashboardSnapshot } from './hooks/useDashboardSnapshot.js'
import {
  accountKey,
  buildIdempotencyFlow,
  buildSettlementFlow,
  buildTransferFlow,
  FLOW_STEP_DURATION_MS,
  receiverCandidates,
} from './utils/flow.js'
import {
  BANK_IDS,
  formatMoney,
  formatTime,
  institutionLabel,
  statusLabel,
  statusTone,
} from './utils/snapshot.js'

// 대시보드의 기관 흐름도, 이체/결제 조작부, 멱등성 재생 화면을 한곳에서 구성한다.
const newId = (prefix) => `${prefix}-${Date.now()}-${crypto.randomUUID().slice(0, 8)}`

const wait = (milliseconds) => new Promise((resolve) => window.setTimeout(resolve, milliseconds))

function StatusPill({ status }) {
  const tone = statusTone(status)
  return <span className={`status-pill status-${tone}`}><i aria-hidden="true" />{statusLabel(status)}</span>
}

function BrandMark() {
  return <span className="brand-mark" aria-hidden="true"><i /><i /><i /></span>
}

function Header({ snapshot, snapshotError }) {
  const healthyCount = snapshot.institutions.filter((item) => statusTone(item.status) === 'success').length
  const summaryTone = snapshotError || healthyCount === 0
    ? 'has-error'
    : healthyCount < snapshot.institutions.length ? 'is-degraded' : ''
  return (
    <header className="app-header">
      <a className="brand" href="#main">
        <BrandMark />
        <span><strong>Financial Infrastructure</strong><small>Payment &amp; settlement lab</small></span>
      </a>
      <div className={`system-summary ${summaryTone}`}>
        <span className="live-dot" aria-hidden="true" />
        <span>{snapshotError ? 'Dashboard 연결 확인 필요' : `기관 ${healthyCount}/${snapshot.institutions.length} 연결`}</span>
      </div>
    </header>
  )
}

function InstitutionNode({ institution, role, account, active, bypassed = false, pendingInstructionCount, queueState = 'idle' }) {
  const showsClearingQueue = institution?.id === 'KFTC' && Number.isInteger(pendingInstructionCount)

  return (
    <article className={`institution-node ${active ? 'is-active' : ''} ${bypassed ? 'is-bypassed' : ''}`} data-tone={statusTone(institution?.status)}>
      <div className="node-heading">
        <span className="node-icon" aria-hidden="true">{institution?.id === 'KFTC' ? '↔' : institution?.id === 'BOK' ? '₩' : 'B'}</span>
        {bypassed ? <span className="bypass-badge">미경유</span> : <StatusPill status={active ? 'PROCESSING' : institution?.status} />}
      </div>
      <div>
        <p className="eyebrow">{role ?? institution?.id}</p>
        <h3>{institution?.name ?? '기관 정보 대기'}</h3>
      </div>
      {account && <div className="account-summary"><span>{account.accountNumber}</span><strong>{formatMoney(account.balance)}</strong></div>}
      {!account && (showsClearingQueue ? (
        <div className={`clearing-queue is-${queueState} ${pendingInstructionCount === 0 ? 'is-empty' : ''}`}>
          <span className="clearing-queue-icon" aria-hidden="true">▤</span>
          <span><small>청산 대기 지급지시</small><strong>{pendingInstructionCount}<em>건</em></strong></span>
        </div>
      ) : <p className="node-role">기관 당좌예금 최종결제</p>)}
    </article>
  )
}

const BANK_NETWORK_X = { BANK_A: 135, BANK_B: 450, BANK_C: 765 }

function BankNode({ institution, senderAccount, receiverAccount, active, isSender, isReceiver, settlementMode, participating }) {
  const accounts = [
    isSender && senderAccount ? { role: '송신', account: senderAccount } : null,
    isReceiver && receiverAccount ? { role: '수신', account: receiverAccount } : null,
  ].filter(Boolean)

  return (
    <article className={`institution-node bank-network-node ${active ? 'is-active' : ''}`} data-tone={statusTone(institution?.status)}>
      <div className="node-heading">
        <span className="node-icon" aria-hidden="true">B</span>
        <StatusPill status={active ? 'PROCESSING' : institution?.status} />
      </div>
      <div>
        <p className="eyebrow">{institution?.id}</p>
        <h3>{institution?.name ?? '은행 정보 대기'}</h3>
      </div>
      {settlementMode ? (
        <p className={`bank-participation ${participating ? 'is-participating' : ''}`}>
          {participating ? '이번 청산 참여 · 완료통보 대상' : '이번 청산 미참여'}
        </p>
      ) : accounts.length ? (
        <div className="bank-account-list">
          {accounts.map(({ role, account }) => (
            <div className="bank-account-row" key={`${role}-${account.accountNumber}`}>
              <span>{role}</span><code>{account.accountNumber}</code><strong>{formatMoney(account.balance)}</strong>
            </div>
          ))}
        </div>
      ) : <p className="node-role">현재 이체 경로에 포함되지 않은 은행</p>}
      {!settlementMode && (isSender || isReceiver) && (
        <div className="bank-role-tags">
          {isSender && <span>송신은행</span>}{isReceiver && <span>수신은행</span>}
        </div>
      )}
    </article>
  )
}

function NetworkPacket({ d, label, pathKey }) {
  const width = Math.max(76, label.length * 13 + 28)

  return (
    <g className="network-packet" key={pathKey}>
      <rect className="network-packet-shape" x={-width / 2} y="-15" width={width} height="30" rx="15" />
      <text className="network-packet-text" x="0" y="4" textAnchor="middle">{label}</text>
      <animateMotion dur={`${FLOW_STEP_DURATION_MS}ms`} path={d} repeatCount="indefinite" />
    </g>
  )
}

function NetworkPath({ d, label, active = false, preview = false, pathKey, markerPrefix = 'network' }) {
  const marker = active ? `url(#${markerPrefix}-arrow-active)` : `url(#${markerPrefix}-arrow-preview)`
  return (
    <g>
      <path className={`network-route ${active ? 'is-active' : ''} ${preview ? 'is-preview' : ''}`} d={d} markerEnd={marker} />
      {active && label && <NetworkPacket d={d} label={label} pathKey={pathKey} />}
    </g>
  )
}

function BokSettlementConnector({ focus }) {
  const isRequest = focus === 'kftc-to-bok'
  const isResponse = focus === 'bok-to-kftc'
  const activePath = isRequest
    ? { d: 'M 110 94 L 110 10', label: '결제요청', key: 'kftc-to-bok' }
    : { d: 'M 110 10 L 110 94', label: '결제 성공결과', key: 'bok-to-kftc' }

  return (
    <svg className="bok-network-svg" viewBox="0 0 220 104" role="img" aria-label={isRequest ? '금융결제원에서 한국은행으로 결제요청' : isResponse ? '한국은행에서 금융결제원으로 결제 성공결과' : '한국은행과 금융결제원 결제망'}>
      <defs>
        <marker id="bok-arrow-preview" markerHeight="8" markerWidth="8" orient="auto" refX="7" refY="4"><path d="M0,0 L8,4 L0,8 Z" /></marker>
        <marker id="bok-arrow-active" markerHeight="9" markerWidth="9" orient="auto" refX="8" refY="4.5"><path d="M0,0 L9,4.5 L0,9 Z" /></marker>
      </defs>
      <path className="network-spoke" d="M 110 10 L 110 94" />
      {(isRequest || isResponse) && <NetworkPath {...activePath} active pathKey={activePath.key} markerPrefix="bok" />}
    </svg>
  )
}

function bankHubPath(bankId, direction) {
  const bankX = BANK_NETWORK_X[bankId]
  return direction === 'bank-to-kftc'
    ? `M ${bankX} 292 C ${bankX} 254 450 252 450 220`
    : `M 450 220 C 450 252 ${bankX} 254 ${bankX} 292`
}

function bankLoopPath(bankId) {
  const bankX = BANK_NETWORK_X[bankId]
  return `M ${bankX - 58} 300 C ${bankX - 122} 220 ${bankX + 122} 220 ${bankX + 58} 300`
}

function BankNetworkMap({ institutions, senderInstitutionId, receiverInstitutionId, senderAccount, receiverAccount, flow, focus, stepIndex, activeBankIds, isDuplicateReplay, pendingInstructionCount, queueState }) {
  const settlementMode = flow.kind === 'settlement' || (isDuplicateReplay && flow.target === 'settlement')
  const internal = senderInstitutionId === receiverInstitutionId && !settlementMode
  const activePaths = []

  if (internal && ['sender', 'internal-receiver'].includes(focus)) {
    activePaths.push({ key: `loop-${senderInstitutionId}-${focus}`, d: bankLoopPath(senderInstitutionId), label: focus === 'sender' ? '예금차감' : '계좌입금' })
  } else if (focus === 'sender-to-kftc') {
    activePaths.push({ key: `${senderInstitutionId}-to-kftc`, d: bankHubPath(senderInstitutionId, 'bank-to-kftc'), label: '지급지시' })
  } else if (focus === 'kftc-to-receiver') {
    activePaths.push({ key: `kftc-to-${receiverInstitutionId}`, d: bankHubPath(receiverInstitutionId, 'kftc-to-bank'), label: '입금지시' })
  } else if (focus === 'receiver-to-kftc') {
    activePaths.push({ key: `${receiverInstitutionId}-to-kftc`, d: bankHubPath(receiverInstitutionId, 'bank-to-kftc'), label: '입금완료 응답' })
  } else if (focus === 'kftc-to-sender') {
    activePaths.push({ key: `kftc-to-${senderInstitutionId}`, d: bankHubPath(senderInstitutionId, 'kftc-to-bank'), label: '이체완료 정보' })
  } else if (focus === 'kftc-to-banks') {
    activeBankIds.forEach((bankId) => activePaths.push({ key: `settlement-${bankId}`, d: bankHubPath(bankId, 'kftc-to-bank'), label: '결제완료 통보' }))
  }

  const previewPaths = []
  if (stepIndex < 0 || isDuplicateReplay) {
    if (settlementMode) {
      activeBankIds.forEach((bankId) => previewPaths.push({ key: `preview-settlement-${bankId}`, d: bankHubPath(bankId, 'kftc-to-bank') }))
    } else if (internal) {
      previewPaths.push({ key: `preview-loop-${senderInstitutionId}`, d: bankLoopPath(senderInstitutionId) })
    } else {
      previewPaths.push(
        { key: `preview-${senderInstitutionId}-kftc`, d: bankHubPath(senderInstitutionId, 'bank-to-kftc') },
        { key: `preview-kftc-${receiverInstitutionId}`, d: bankHubPath(receiverInstitutionId, 'kftc-to-bank') },
      )
    }
  }

  const routeLabel = settlementMode
    ? `KFTC → ${activeBankIds.map(institutionLabel).join(' · ') || '참여은행 확인 중'}`
    : internal
      ? `${institutionLabel(senderInstitutionId)} 내부 계좌 간 이체`
      : `${institutionLabel(senderInstitutionId)} → 금융결제원 → ${institutionLabel(receiverInstitutionId)}`

  function bankActive(bankId) {
    if (settlementMode) return activeBankIds.includes(bankId) && (focus === 'kftc-to-banks' || isDuplicateReplay)
    if (isDuplicateReplay) return bankId === senderInstitutionId || bankId === receiverInstitutionId
    if (['sender', 'sender-to-kftc', 'kftc-to-sender', 'sender-complete'].includes(focus)) return bankId === senderInstitutionId
    if (['internal-receiver', 'kftc-to-receiver', 'receiver-to-kftc'].includes(focus)) return bankId === receiverInstitutionId
    return false
  }

  return (
    <div className="bank-network-wrap">
      <div className="network-route-caption"><span>현재 경로</span><strong>{routeLabel}</strong></div>
      <div className="bank-network-map">
        <svg className="bank-network-svg" viewBox="0 0 900 445" role="img" aria-label={routeLabel}>
          <defs>
            <marker id="network-arrow-preview" markerHeight="8" markerWidth="8" orient="auto" refX="7" refY="4"><path d="M0,0 L8,4 L0,8 Z" /></marker>
            <marker id="network-arrow-active" markerHeight="9" markerWidth="9" orient="auto" refX="8" refY="4.5"><path d="M0,0 L9,4.5 L0,9 Z" /></marker>
          </defs>
          {BANK_IDS.map((bankId) => <path className="network-spoke" d={bankHubPath(bankId, 'kftc-to-bank')} key={`spoke-${bankId}`} />)}
          {previewPaths.map((path) => <NetworkPath {...path} preview pathKey={path.key} key={path.key} />)}
          {activePaths.map((path) => <NetworkPath {...path} active pathKey={path.key} key={path.key} />)}
        </svg>
        <div className="network-kftc">
          <InstitutionNode
            institution={institutions.KFTC}
            role="청산기관"
            active={focus?.includes('kftc') || settlementMode}
            bypassed={internal}
            pendingInstructionCount={pendingInstructionCount}
            queueState={queueState}
          />
        </div>
        {BANK_IDS.map((bankId) => (
          <div className={`network-bank network-${bankId.toLowerCase().replace('_', '-')}`} key={bankId}>
            <BankNode
              institution={institutions[bankId]}
              senderAccount={bankId === senderInstitutionId ? senderAccount : null}
              receiverAccount={bankId === receiverInstitutionId ? receiverAccount : null}
              active={bankActive(bankId)}
              isSender={!settlementMode && bankId === senderInstitutionId}
              isReceiver={!settlementMode && bankId === receiverInstitutionId}
              settlementMode={settlementMode}
              participating={activeBankIds.includes(bankId)}
            />
          </div>
        ))}
      </div>
    </div>
  )
}

function InstitutionStatusRail({ institutions, activeBanks }) {
  return (
    <div className="institution-status-rail" aria-label="참여 기관 연결 상태">
      {institutions.map((institution) => (
        <div className={activeBanks.includes(institution.id) ? 'is-active' : ''} key={institution.id}>
          <span className={`rail-status status-${statusTone(institution.status)}`} aria-hidden="true" />
          <strong>{institutionLabel(institution.id)}</strong>
          <small>{statusLabel(institution.status)}</small>
        </div>
      ))}
    </div>
  )
}

function ProcessSteps({ flow, stepIndex }) {
  return (
    <ol className={`process-steps ${flow.kind === 'idempotency-replay' ? 'is-idempotency' : ''}`} aria-label={`${flow.label} 처리 단계`}>
      {flow.steps.map((step, index) => {
        const state = stepIndex < 0 ? 'waiting' : index < stepIndex ? 'complete' : index === stepIndex ? 'current' : 'waiting'
        return (
          <li className={`is-${state}`} aria-current={state === 'current' ? 'step' : undefined} key={step.id}>
            <span>{state === 'complete' ? '✓' : step.id}</span>
            <div><strong>{step.title}</strong><p>{step.description}</p></div>
          </li>
        )
      })}
    </ol>
  )
}

function InfrastructureFlow({ snapshot, operation, lastTransfer, pendingInstructionCount }) {
  // 빠르게 끝난 실제 API 결과를 operation의 단계 번호에 맞춰 느린 교육용 흐름으로 표현한다.
  const institutions = Object.fromEntries(snapshot.institutions.map((item) => [item.id, item]))
  const route = operation?.senderInstitutionId ? operation : lastTransfer?.payload
  const senderInstitutionId = route?.senderInstitutionId ?? 'BANK_A'
  const receiverInstitutionId = route?.receiverInstitutionId ?? 'BANK_B'
  const senderAccount = snapshot.accounts.find((account) => account.institutionId === senderInstitutionId && account.accountNumber === route?.senderAccountNumber)
    ?? snapshot.accounts.find((account) => account.institutionId === senderInstitutionId)
  const receiverAccount = snapshot.accounts.find((account) => account.institutionId === receiverInstitutionId && account.accountNumber === route?.receiverAccountNumber)
    ?? snapshot.accounts.find((account) => account.institutionId === receiverInstitutionId)
  const flow = operation?.flow ?? buildTransferFlow(senderInstitutionId, receiverInstitutionId)
  const stepIndex = operation?.stepIndex ?? -1
  const focus = flow.steps[stepIndex]?.focus
  const isDuplicateReplay = flow.kind === 'idempotency-replay'
  const duplicateTarget = flow.target
  const isInternal = flow.kind === 'internal-transfer'
    || (isDuplicateReplay && duplicateTarget === 'transfer' && senderInstitutionId === receiverInstitutionId)
  const routeBankIds = [senderInstitutionId, receiverInstitutionId]
    .filter((id, index, values) => values.indexOf(id) === index)
  const activeBanks = flow.kind === 'settlement' || (isDuplicateReplay && duplicateTarget === 'settlement')
    ? operation?.activeBankIds ?? routeBankIds
    : routeBankIds
  const bokActive = focus?.includes('bok')
  const settlementPlayback = flow.kind === 'settlement' && operation
  const transferPlayback = flow.kind === 'interbank-transfer' && operation
  const visiblePendingInstructionCount = settlementPlayback
    ? focus === 'kftc-to-banks' ? 0 : operation.pendingInstructionCountBefore ?? pendingInstructionCount
    : transferPlayback
      ? stepIndex >= 3
        ? operation.pendingInstructionCountAfter ?? pendingInstructionCount
        : operation.pendingInstructionCountBefore ?? pendingInstructionCount
      : pendingInstructionCount
  const queueState = settlementPlayback
    ? focus === 'kftc-to-banks' ? 'cleared' : 'processing'
    : transferPlayback && stepIndex >= 3 ? 'updated' : 'idle'

  return (
    <section className="panel flow-panel" aria-labelledby="flow-title">
      <div className="section-heading flow-heading">
        <div><p className="eyebrow">PAYMENT FLOW MAP</p><h2 id="flow-title">자금이 움직이는 전체 과정</h2></div>
        <div className="playback-note"><span>{flow.label}</span><p>{flow.description}</p></div>
      </div>
      <InstitutionStatusRail institutions={snapshot.institutions} activeBanks={activeBanks} />
      <div className={`flow-canvas ${isInternal ? 'is-internal' : ''}`}>
        <div className="bok-node">
          <InstitutionNode institution={institutions.BOK} role="결제기관" active={bokActive} bypassed={isInternal || flow.requiresBok === false} />
        </div>
        <div className="bok-connector">
          <BokSettlementConnector focus={focus} />
        </div>
        <BankNetworkMap
          institutions={institutions}
          senderInstitutionId={senderInstitutionId}
          receiverInstitutionId={receiverInstitutionId}
          senderAccount={senderAccount}
          receiverAccount={receiverAccount}
          flow={flow}
          focus={focus}
          stepIndex={stepIndex}
          activeBankIds={activeBanks}
          isDuplicateReplay={isDuplicateReplay}
          pendingInstructionCount={visiblePendingInstructionCount}
          queueState={queueState}
        />
        {isDuplicateReplay && operation && <div className={`duplicate-guard is-${focus}`}><span aria-hidden="true">↺</span><div><strong>동일 요청 감지 · 중복 반영 차단</strong><p>현재 확인: {flow.steps[stepIndex]?.title} — 고객예금, 지급지시, 원장과 결제 잔액은 다시 변경되지 않습니다.</p></div></div>}
        {operation && <div className="active-step-announcement" role="status" aria-live="polite"><span>STEP {stepIndex + 1}</span><strong>{flow.steps[stepIndex]?.title}</strong><small>업무 흐름을 설명하기 위한 단계별 재생입니다.</small></div>}
      </div>
      <ProcessSteps flow={flow} stepIndex={stepIndex} />
    </section>
  )
}

function TransferComposer({ accounts, busy, submitting, onSubmit, canReplay, onReplay }) {
  const usableAccounts = accounts.filter((item) => item.institutionId && item.accountNumber)
  const [senderKey, setSenderKey] = useState('')
  const [receiverKey, setReceiverKey] = useState('')
  const [amount, setAmount] = useState('30000')
  const sender = usableAccounts.find((item) => accountKey(item) === senderKey) ?? usableAccounts[0]
  const receivers = receiverCandidates(usableAccounts, sender)
  const receiver = receivers.find((item) => accountKey(item) === receiverKey) ?? receivers[0]
  const isInternal = sender && receiver && sender.institutionId === receiver.institutionId

  function submit(event) {
    event.preventDefault()
    if (!sender || !receiver) return
    onSubmit({
      requestId: newId('transfer'),
      senderInstitutionId: sender.institutionId,
      senderCustomerId: sender.customerId,
      senderAccountNumber: sender.accountNumber,
      receiverInstitutionId: receiver.institutionId,
      receiverAccountNumber: receiver.accountNumber,
      amount: Number(amount),
    })
  }

  return (
    <section className="panel action-panel" aria-labelledby="transfer-title">
      <div className="section-heading compact"><div><p className="eyebrow">TRANSFER</p><h2 id="transfer-title">계좌 이체</h2></div>{sender && receiver && <span className={`route-badge ${isInternal ? 'is-internal' : ''}`}>{isInternal ? '은행 내 이체' : '은행 간 이체'}</span>}</div>
      <form onSubmit={submit} className="transfer-form">
        <label>송신 계좌
          <select value={sender ? accountKey(sender) : ''} onChange={(event) => { setSenderKey(event.target.value); setReceiverKey('') }} disabled={!usableAccounts.length}>
            {!usableAccounts.length && <option>계좌 정보가 없습니다</option>}
            {usableAccounts.map((account) => <option key={accountKey(account)} value={accountKey(account)}>{institutionLabel(account.institutionId)} · {account.customerName} · {account.accountNumber} ({formatMoney(account.balance)})</option>)}
          </select>
        </label>
        <span className="form-arrow" aria-hidden="true">→</span>
        <label>수신 계좌
          <select value={receiver ? accountKey(receiver) : ''} onChange={(event) => setReceiverKey(event.target.value)} disabled={!receivers.length}>
            {!receivers.length && <option>선택 가능한 수신 계좌가 없습니다</option>}
            {receivers.map((account) => <option key={accountKey(account)} value={accountKey(account)}>{institutionLabel(account.institutionId)} · {account.customerName} · {account.accountNumber}</option>)}
          </select>
        </label>
        <label className="amount-field">이체 금액
          <div className="input-suffix"><input type="number" min="1" step="1" value={amount} onChange={(event) => setAmount(event.target.value)} /><span>원</span></div>
        </label>
        <button className="button primary" type="submit" disabled={busy || !sender || !receiver || Number(amount) <= 0}>{submitting ? '이체 처리 중…' : busy ? '흐름 재생 중…' : '이체 실행'}</button>
        <button className="button secondary" type="button" disabled={busy || !canReplay} onClick={onReplay}>동일 이체요청 재전송</button>
      </form>
      <p className="form-help">같은 은행의 다른 계좌를 선택하면 KFTC·BOK를 거치지 않는 은행 내 이체로 처리됩니다. 재전송은 마지막 requestId의 멱등성을 확인합니다.</p>
    </section>
  )
}

function SettlementControl({ instructionCount, busy, submitting, onSubmit, canReplay, onReplay }) {
  const [cycleId, setCycleId] = useState(() => newId('cycle'))

  async function submitCycle() {
    const succeeded = await onSubmit(cycleId.trim())
    if (succeeded) setCycleId(newId('cycle'))
  }

  return (
    <section className="panel settlement-control" aria-labelledby="settlement-title">
      <div className="section-heading compact"><div><p className="eyebrow">CLEARING &amp; SETTLEMENT</p><h2 id="settlement-title">청산 및 결제</h2></div><span className="count-badge">대상 {instructionCount}건</span></div>
      <label>청산주기 ID<input value={cycleId} onChange={(event) => setCycleId(event.target.value)} /></label>
      <div className="settlement-actions">
        <button className="button dark" type="button" disabled={busy || !cycleId.trim() || instructionCount === 0} onClick={submitCycle}>{submitting ? '청산·결제 처리 중…' : busy ? '결제 흐름 재생 중…' : '청산 및 결제 실행'}</button>
        <button className="button secondary" type="button" disabled={busy || !canReplay} onClick={onReplay}>동일 결제요청 재전송</button>
      </div>
      <p className="form-help">입금이 완료된 지급지시를 모아 순포지션을 계산하고 BOK 결제를 요청합니다.</p>
    </section>
  )
}

function AccountsPanel({ accounts, settlementAccounts }) {
  return (
    <section className="panel data-panel">
      <div className="section-heading compact"><div><p className="eyebrow">BALANCES</p><h2>잔액 현황</h2></div></div>
      <div className="balance-groups">
        <div><h3>은행 고객예금</h3>{accounts.length ? accounts.map((account) => <div className="balance-row" key={`${account.institutionId}-${account.accountNumber}`}><span><b>{institutionLabel(account.institutionId)}</b><small>{account.customerName || account.accountNumber}</small></span><strong>{formatMoney(account.balance)}</strong></div>) : <Empty text="조회된 고객계좌가 없습니다." />}</div>
        <div><h3>BOK 당좌예금</h3>{settlementAccounts.length ? settlementAccounts.map((account, index) => <div className="balance-row" key={account.accountNumber ?? `${account.institutionId}-${index}`}><span><b>{institutionLabel(account.institutionId ?? account.bankId)}</b><small>{account.accountNumber ?? '기관 결제계좌'}</small></span><strong>{formatMoney(account.balance ?? account.balanceAmount)}</strong></div>) : <Empty text="조회된 당좌예금이 없습니다." />}</div>
      </div>
    </section>
  )
}

function InstructionTable({ instructions }) {
  return (
    <section className="panel table-panel">
      <div className="section-heading compact"><div><p className="eyebrow">PAYMENT INSTRUCTIONS</p><h2>지급지시</h2></div><span className="count-badge">{instructions.length}건</span></div>
      {instructions.length ? <div className="table-scroll"><table><thead><tr><th>Instruction ID</th><th>경로</th><th>금액</th><th>상태</th></tr></thead><tbody>{instructions.map((item, index) => <tr key={item.instructionId ?? item.id ?? index}><td className="mono">{item.instructionId ?? item.id ?? '-'}</td><td>{institutionLabel(item.senderInstitutionId ?? item.senderInstitution)} → {institutionLabel(item.receiverInstitutionId ?? item.receiverInstitution)}</td><td>{formatMoney(item.amount ?? item.amountValue)}</td><td><StatusPill status={item.status} /></td></tr>)}</tbody></table></div> : <Empty text="아직 지급지시가 없습니다. 위에서 첫 이체를 실행해보세요." />}
    </section>
  )
}

function ClearingPanel({ clearing, positions }) {
  const status = clearing?.status ?? clearing?.cycleStatus
  return (
    <section className="panel data-panel">
      <div className="section-heading compact"><div><p className="eyebrow">NET POSITIONS</p><h2>기관별 순포지션</h2></div>{status && <StatusPill status={status} />}</div>
      {positions.length ? <div className="position-list">{positions.map((position, index) => { const amount = Number(position.netAmount ?? position.amount ?? 0); return <div className="position-row" key={position.institutionId ?? index}><span>{institutionLabel(position.institutionId)}</span><strong className={amount < 0 ? 'negative' : 'positive'}>{amount > 0 ? '+' : ''}{formatMoney(amount)}</strong></div> })}</div> : <Empty text="청산 전입니다. 지급지시가 모이면 순포지션을 계산할 수 있습니다." />}
      {(clearing?.cycleId || clearing?.clearingCycleId) && <div className="identifier"><span>Cycle ID</span><code>{clearing.cycleId ?? clearing.clearingCycleId}</code></div>}
      {(clearing?.batchId || clearing?.settlementBatchId || clearing?.submissionId) && <div className="identifier"><span>Batch ID</span><code>{clearing.batchId ?? clearing.settlementBatchId ?? clearing.submissionId}</code></div>}
    </section>
  )
}

function Timeline({ transactions }) {
  return (
    <section className="panel timeline-panel">
      <div className="section-heading compact"><div><p className="eyebrow">RECENT ACTIVITY</p><h2>처리 타임라인</h2></div></div>
      {transactions.length ? <ol className="timeline">{transactions.slice(0, 8).map((item, index) => <li key={item.eventId ?? item.transactionId ?? item.id ?? index}><span className={`timeline-dot status-${statusTone(item.status)}`} /><div><strong>{item.title ?? item.type ?? item.description ?? '거래 상태 변경'}</strong><p>{item.message ?? item.requestId ?? item.instructionId ?? '-'}</p></div><time>{formatTime(item.occurredAt ?? item.createdAt ?? item.updatedAt)}</time></li>)}</ol> : <Empty text="아직 기록된 거래가 없습니다." />}
    </section>
  )
}

function Empty({ text }) { return <p className="empty-state">{text}</p> }

export default function App() {
  const { snapshot, loading, error, refresh } = useDashboardSnapshot()
  const [operation, setOperation] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [lastTransfer, setLastTransfer] = useState(null)
  const [lastSettlement, setLastSettlement] = useState(null)
  const [notice, setNotice] = useState(null)
  const pendingCount = useMemo(() => pendingInstructionCount(snapshot.instructions), [snapshot.instructions])

  async function playFlow(flow, detail = {}) {
    for (let stepIndex = 0; stepIndex < flow.steps.length; stepIndex += 1) {
      setOperation({ ...detail, type: flow.kind, flow, stepIndex })
      const isIdempotencyResult = flow.kind === 'idempotency-replay' && stepIndex === flow.steps.length - 1
      await wait(isIdempotencyResult ? FLOW_STEP_DURATION_MS * 2 : FLOW_STEP_DURATION_MS)
    }
  }

  async function submitSettlement(cycleId, replay = false) {
    // 일반 실행은 새 cycleId를 사용하고, replay는 직전 cycleId로 상태 불변까지 검증한다.
    setNotice(null)
    setSubmitting(true)
    const stateBefore = settlementSignature(snapshot)
    let succeeded = false
    let flowDetail = {
      activeBankIds: lastSettlement?.activeBankIds ?? [],
      requiresBok: lastSettlement?.requiresBok ?? true,
      pendingInstructionCountBefore: pendingCount,
    }
    try {
      const result = await settleClearingCycle(cycleId)
      const refreshed = await refresh()
      const resolvedCycleId = result?.cycleId ?? cycleId
      const activeBankIds = replay
        ? lastSettlement?.activeBankIds ?? []
        : participantBankIds(refreshed, resolvedCycleId)
      const requiresBok = replay
        ? lastSettlement?.requiresBok ?? true
        : !Array.isArray(result?.positions) || result.positions.length > 0
      flowDetail = { activeBankIds, requiresBok, pendingInstructionCountBefore: pendingCount }
      if (replay) {
        const verified = result?.cycleId === lastSettlement?.cycleId
          && result?.submissionId === lastSettlement?.submissionId
          && refreshed
          && settlementSignature(refreshed) === stateBefore
        if (!verified) throw new Error('동일 결제요청의 중복 미반영을 검증하지 못했습니다.')
        setNotice({ tone: 'success', message: '동일 청산주기 ID를 감지해 결제·원장·잔액의 중복 반영을 차단했습니다.' })
      } else {
        setLastSettlement({
          cycleId: resolvedCycleId,
          submissionId: result?.submissionId,
          activeBankIds,
          requiresBok,
        })
        setNotice({ tone: 'success', message: '청산과 BOK 최종결제가 완료되었습니다.' })
      }
      succeeded = true
    } catch (requestError) {
      setNotice({ tone: 'failure', message: requestError.message })
    } finally {
      setSubmitting(false)
    }
    if (succeeded) {
      await playFlow(replay ? buildIdempotencyFlow('settlement') : buildSettlementFlow(flowDetail.requiresBok), flowDetail)
      setOperation(null)
    }
    return succeeded
  }

  async function submitTransfer(payload, replay = false) {
    // 재전송 시 거래 ID뿐 아니라 잔액·지급지시·원장 스냅샷이 그대로인지 확인한다.
    setNotice(null)
    setSubmitting(true)

    const stateBefore = transferSignature(snapshot)
    const pendingInstructionCountBefore = pendingCount
    let succeeded = false
    let pendingInstructionCountAfter = pendingCount
    try {
      const result = await createTransfer(payload)
      const refreshed = await refresh()
      pendingInstructionCountAfter = pendingInstructionCount(refreshed?.instructions)

      if (replay) {
        const verified = result?.transactionId === lastTransfer?.transactionId
          && refreshed
          && transferSignature(refreshed) === stateBefore
        if (!verified) throw new Error('동일 요청의 중복 미반영을 검증하지 못했습니다.')
        setNotice({ tone: 'success', message: '같은 transactionId와 변경 없는 잔액·지급지시 수를 확인했습니다.' })
      } else {
        setLastTransfer({ payload, transactionId: result?.transactionId })
        const internal = payload.senderInstitutionId === payload.receiverInstitutionId
        setNotice({ tone: 'success', message: internal ? '은행 내 이체가 완료되었습니다.' : '수신은행 입금 처리가 완료되었습니다.' })
      }
      succeeded = true
    } catch (requestError) {
      setNotice({ tone: 'failure', message: requestError.message })
    } finally {
      setSubmitting(false)
    }
    if (succeeded) {
      await playFlow(
        replay ? buildIdempotencyFlow('transfer') : buildTransferFlow(payload.senderInstitutionId, payload.receiverInstitutionId),
        { ...payload, pendingInstructionCountBefore, pendingInstructionCountAfter },
      )
      setOperation(null)
    }
  }

  return (
    <div className="app-shell">
      <Header snapshot={snapshot} snapshotError={error} />
      <main id="main">
        <section className="hero">
          <div><p className="eyebrow">PAYMENT OPERATIONS CONSOLE</p><h1>계좌 이체부터<br />최종 결제까지 한눈에.</h1><p>은행 내 이체와 은행 간 지급지시, KFTC 순액청산, BOK 당좌예금 결제를 실제 기관 API 흐름으로 관찰합니다.</p></div>
          <div className="hero-meta"><span>마지막 상태 갱신</span><strong>{loading ? '연결 중…' : formatTime(snapshot.updatedAt)}</strong><small>2초 간격 자동 갱신</small></div>
        </section>
        {error && <div className="alert failure" role="alert"><strong>Dashboard 데이터를 불러오지 못했습니다.</strong><span>{error.message}</span><button type="button" onClick={refresh}>다시 시도</button></div>}
        {notice && <div className={`alert ${notice.tone}`} role="status" aria-live="polite"><strong>{notice.tone === 'success' ? '처리 완료' : '처리 실패'}</strong><span>{notice.message}</span><button type="button" onClick={() => setNotice(null)} aria-label="알림 닫기">×</button></div>}
        <InfrastructureFlow snapshot={snapshot} operation={operation} lastTransfer={lastTransfer} pendingInstructionCount={pendingCount} />
        <div className="operation-grid">
          <TransferComposer accounts={snapshot.accounts} busy={submitting || Boolean(operation)} submitting={submitting} onSubmit={submitTransfer} canReplay={Boolean(lastTransfer)} onReplay={() => submitTransfer(lastTransfer.payload, true)} />
          <SettlementControl instructionCount={pendingCount} busy={submitting || Boolean(operation)} submitting={submitting} onSubmit={submitSettlement} canReplay={Boolean(lastSettlement)} onReplay={() => submitSettlement(lastSettlement.cycleId, true)} />
        </div>
        <div className="dashboard-grid">
          <AccountsPanel accounts={snapshot.accounts} settlementAccounts={snapshot.settlementAccounts} />
          <ClearingPanel clearing={snapshot.clearing} positions={snapshot.positions} />
          <InstructionTable instructions={snapshot.instructions} />
          <Timeline transactions={snapshot.transactions} />
        </div>
      </main>
      <footer><span>Financial Infrastructure Lab</span><span>Bank A · Bank B · Bank C · KFTC · BOK</span></footer>
    </div>
  )
}

function balanceSignature(accounts) {
  return accounts
    .map((account) => `${account.institutionId}:${account.accountNumber}:${account.balance}`)
    .sort()
    .join('|')
}

function pendingInstructionCount(instructions = []) {
  return instructions.filter(
    (item) => String(item.status).toUpperCase() === 'RECEIVER_CREDITED' && !item.clearingCycleId,
  ).length
}

function settlementSignature(snapshot) {
  const accounts = snapshot.settlementAccounts
    .map((account) => `${account.institutionId ?? account.bankId}:${account.balance ?? account.balanceAmount}`)
    .sort()
    .join('|')
  const instructions = snapshot.instructions
    .map((instruction) => `${instruction.instructionId ?? instruction.id}:${instruction.status}:${instruction.clearingCycleId ?? ''}`)
    .sort()
    .join('|')
  const cycles = (snapshot.clearingCycles ?? []).map((cycle) => `${cycle.cycleId}:${cycle.status}`).sort().join('|')
  const submissions = (snapshot.settlementSubmissions ?? []).map((submission) => `${submission.submissionId}:${submission.status}`).sort().join('|')
  const batches = (snapshot.settlementBatches ?? []).map((batch) => `${batch.batchId}:${batch.status}`).sort().join('|')
  const ledgers = (snapshot.settlementLedgerEntries ?? []).map((entry) => `${entry.ledgerEntryId}:${entry.balanceAfter}`).sort().join('|')
  return `${accounts}::${instructions}::${cycles}::${submissions}::${batches}::${ledgers}`
}

function transferSignature(snapshot) {
  const ledgers = (snapshot.ledgerEntries ?? [])
    .map((entry) => `${entry.institutionId}:${entry.ledgerEntryId}:${entry.balanceAfter}`)
    .sort()
    .join('|')
  return `${balanceSignature(snapshot.accounts)}::${snapshot.instructions.length}::${ledgers}`
}

function participantBankIds(snapshot, cycleId) {
  return snapshot.instructions
    .filter((instruction) => (instruction.clearingCycleId ?? '') === cycleId)
    .flatMap((instruction) => [
      instruction.senderInstitutionId ?? instruction.senderInstitution,
      instruction.receiverInstitutionId ?? instruction.receiverInstitution,
    ])
    .filter((institutionId, index, values) => institutionId?.startsWith('BANK_') && values.indexOf(institutionId) === index)
}
