import { useCallback, useEffect, useState } from 'react'
import { fetchSnapshot } from '../api/dashboard.js'
import { emptySnapshot, normalizeSnapshot } from '../utils/snapshot.js'

// 기관별 모니터링 스냅샷을 주기적으로 갱신하되, 이전 성공 데이터는 오류 시에도 유지한다.
export function useDashboardSnapshot(intervalMs = 2000) {
  const [snapshot, setSnapshot] = useState(emptySnapshot)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const refresh = useCallback(async (signal) => {
    try {
      const data = await fetchSnapshot(signal)
      const normalized = normalizeSnapshot(data)
      setSnapshot(normalized)
      setError(null)
      return normalized
    } catch (requestError) {
      if (requestError.name !== 'AbortError') setError(requestError)
      return null
    } finally {
      if (!signal?.aborted) setLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    let timer
    let disposed = false

    async function poll() {
      await refresh(controller.signal)
      if (!disposed) timer = window.setTimeout(poll, intervalMs)
    }

    poll()
    return () => {
      disposed = true
      controller.abort()
      window.clearTimeout(timer)
    }
  }, [intervalMs, refresh])

  return { snapshot, loading, error, refresh: () => refresh() }
}
