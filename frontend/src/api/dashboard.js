const JSON_HEADERS = { 'Content-Type': 'application/json' }

// 프론트가 dashboard-service 하나만 호출하도록 HTTP 처리와 오류 변환을 모은다.
async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: { ...JSON_HEADERS, ...options.headers },
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `요청을 처리하지 못했습니다. (${response.status})`)
  }

  if (response.status === 204) return null
  const text = await response.text()
  return text ? JSON.parse(text) : null
}

export function fetchSnapshot(signal) {
  return request('/api/dashboard/snapshot', { signal })
}

export function createTransfer(payload) {
  return request('/api/dashboard/transfers', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function settleClearingCycle(cycleId) {
  return request(`/api/dashboard/clearing-cycles/${encodeURIComponent(cycleId)}/settlement`, {
    method: 'POST',
  })
}
