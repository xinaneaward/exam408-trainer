import { ref } from 'vue'

const API_BASE = import.meta.env.VITE_API_BASE || '/api'

/**
 * AI 流式请求（fetch 消费后端 SSE）
 * 后端事件数据为JSON：{ type: 'meta'|'delta'|'done'|'error', ... }
 */
export function useAiStream() {
  const streaming = ref(false)
  let abortController = null

  /**
   * @param {Object} opts
   * @param {string} opts.url 相对API前缀的路径，如 /ai/explain/stream
   * @param {Object} opts.body 请求体
   * @param {Function} [opts.onEvent] 收到事件 (evt)
   * @param {Function} [opts.onError] 出错回调 (message)
   */
  async function stream({ url, body, onEvent, onError }) {
    streaming.value = true
    abortController = new AbortController()
    try {
      const resp = await fetch(API_BASE + url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify(body),
        signal: abortController.signal
      })
      if (!resp.ok || !resp.body) {
        let msg = `请求失败 (HTTP ${resp.status})`
        try {
          const j = await resp.json()
          if (j && j.message) msg = j.message
        } catch (e) { /* ignore */ }
        onError && onError(msg)
        return
      }

      const reader = resp.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buf = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buf += decoder.decode(value, { stream: true })
        const parts = buf.split('\n')
        buf = parts.pop()
        for (const raw of parts) {
          const line = raw.trim()
          if (!line.startsWith('data:')) continue
          const payload = line.slice(5).trim()
          if (!payload || payload === '[DONE]') continue
          try {
            const evt = JSON.parse(payload)
            if (evt.type === 'error') {
              onError && onError(evt.message || 'AI 服务出错')
              return
            }
            onEvent && onEvent(evt)
          } catch (e) { /* 忽略无法解析的行 */ }
        }
      }
      // 流结束但未收到done事件
    } catch (e) {
      if (e.name === 'AbortError') {
        onEvent && onEvent({ type: 'aborted' })
      } else {
        onError && onError('网络异常：' + e.message)
      }
    } finally {
      streaming.value = false
      abortController = null
    }
  }

  function abort() {
    if (abortController) abortController.abort()
  }

  return { streaming, stream, abort }
}
