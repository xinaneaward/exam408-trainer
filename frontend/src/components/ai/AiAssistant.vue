<template>
  <Teleport to="body">
    <!-- 悬浮球（仅登录页外显示） -->
    <button
      v-if="!panelVisible"
      class="ai-fab"
      :class="{ 'ai-fab-has-ctx': hasContext }"
      @click="open"
      title="AI 答疑助手"
    >
      🤖
      <span v-if="hasContext" class="ai-fab-badge">题目</span>
    </button>

    <!-- 对话面板 -->
    <transition name="ai-pop">
      <div v-if="panelVisible" class="ai-pop-panel" :class="theme">
        <div class="ai-pop-head">
          <div class="ai-pop-title">
            <span class="ai-pop-logo">🤖</span>
            <div>
              <div class="ai-pop-name">AI 答疑助手</div>
              <div class="ai-pop-sub" v-if="hasContext">{{ ctxSummary }}</div>
              <div class="ai-pop-sub" v-else>随时问我 408 相关问题</div>
            </div>
          </div>
          <div class="ai-pop-actions">
            <button class="ai-pop-icon" title="清空对话" @click="reset" :disabled="streaming">🗑</button>
            <button class="ai-pop-icon" title="收起" @click="close">▾</button>
          </div>
        </div>

        <div class="ai-pop-body" ref="bodyRef">
          <div v-if="errorMsg" class="ai-pop-tip ai-pop-err">⚠️ {{ errorMsg }}</div>
          <div v-if="!messages.length && !streaming" class="ai-pop-welcome">
            <div class="ai-pop-welcome-icon">🎓</div>
            <div class="ai-pop-welcome-text">你好！我是 408 AI 答疑助手。</div>
            <div class="ai-pop-welcome-hint" v-if="hasContext">
              检测到你正在做题，可以直接问我当前这道题的任何疑问。
            </div>
            <div class="ai-pop-welcome-hint" v-else>
              可以问我数据结构、组成原理、操作系统、计算机网络相关的问题。
            </div>
            <div class="ai-pop-suggest">
              <button v-for="s in suggestions" :key="s" class="ai-pop-chip" @click="quick(s)">{{ s }}</button>
            </div>
          </div>
          <div v-for="(m, i) in messages" :key="i" class="ai-pop-msg" :class="'ai-pop-' + m.role">
            <div class="ai-pop-bubble" v-html="renderMarkdown(m.content)"></div>
          </div>
          <div v-if="waitingFirst" class="ai-pop-typing">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            AI 正在思考…
          </div>
        </div>

        <div class="ai-pop-foot">
          <textarea
            v-model="input"
            class="ai-pop-input"
            rows="2"
            :disabled="streaming"
            placeholder="输入你的问题，Enter 发送"
            @keydown.enter.exact.prevent="send"
          ></textarea>
          <button v-if="streaming" class="ai-pop-send ai-pop-stop" @click="stop">停止</button>
          <button v-else class="ai-pop-send" @click="send" :disabled="!input.trim()">发送</button>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAiContextStore } from '../../stores/aiContext'
import { useAiStream } from '../../composables/useAiStream'
import { renderMarkdown } from '../../utils/markdown'

const route = useRoute()
const aiCtx = useAiContextStore()
const { streaming, stream, abort } = useAiStream()

const panelVisible = ref(false)
const messages = ref([])
const sessionId = ref(null)
const errorMsg = ref('')
const input = ref('')
const bodyRef = ref(null)

const suggestions = [
  '什么是虚拟内存？',
  '红黑树和AVL树的区别？',
  'TCP三次握手过程',
  'Cache映射方式有哪些？'
]

const hasContext = computed(() => !!aiCtx.currentQuestion)
const ctxSummary = computed(() => {
  const q = aiCtx.currentQuestion
  if (!q) return ''
  const parts = []
  if (q.year) parts.push(q.year + '年')
  if (q.subject) parts.push(q.subject)
  if (q.knowledgeTag) parts.push(q.knowledgeTag)
  return parts.join(' · ')
})
const theme = computed(() => document.body.classList.contains('dark-mode') ? 'dark' : 'light')

const waitingFirst = computed(() => {
  if (!streaming.value) return false
  const last = messages.value[messages.value.length - 1]
  return last && last.role === 'assistant' && !last.content
})

function open() { panelVisible.value = true }
function close() { panelVisible.value = false }

function quick(text) {
  input.value = text
  send()
}

function reset() {
  if (streaming.value) abort()
  messages.value = []
  sessionId.value = null
  errorMsg.value = ''
  input.value = ''
}

function send() {
  const text = input.value.trim()
  if (!text || streaming.value) return
  input.value = ''
  errorMsg.value = ''
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })
  scrollBottom()

  const ctx = aiCtx.currentQuestion
  const body = sessionId.value
    ? { sessionId: sessionId.value, message: text }
    : { message: text, questionId: ctx ? ctx.id : undefined }

  stream({
    url: sessionId.value ? '/ai/chat/stream' : '/ai/assistant/stream',
    body,
    onEvent: handleEvent,
    onError: handleError
  })
}

function handleEvent(evt) {
  if (evt.type === 'meta') {
    sessionId.value = evt.sessionId
  } else if (evt.type === 'delta') {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content += evt.text
      scrollBottom()
    }
  } else if (evt.type === 'done') {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant' && !last.content) last.content = '(AI 未返回内容)'
  } else if (evt.type === 'aborted') {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant' && !last.content) last.content = '(已停止)'
  }
}

function handleError(msg) {
  errorMsg.value = msg
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant' && !last.content) messages.value.pop()
}

function stop() { abort() }

function scrollBottom() {
  nextTick(() => {
    if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  })
}

// 路由切换时重置对话（避免跨页面带历史）
watch(() => route.path, () => {
  if (panelVisible.value) reset()
})
</script>

<style scoped>
.ai-fab {
  position: fixed; right: 24px; bottom: 24px; z-index: 1500;
  width: 56px; height: 56px; border-radius: 50%;
  border: none; cursor: pointer; font-size: 26px;
  background: linear-gradient(135deg, #7c3aed, #6d28d9);
  color: #fff; box-shadow: 0 6px 20px rgba(124, 58, 237, 0.45);
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.2s ease;
}
.ai-fab:hover { transform: scale(1.08); }
.ai-fab-has-ctx { background: linear-gradient(135deg, #059669, #047857); box-shadow: 0 6px 20px rgba(5, 150, 105, 0.45); }
.ai-fab-badge {
  position: absolute; top: -4px; right: -4px;
  background: #ef4444; color: #fff; font-size: 10px; font-weight: 700;
  padding: 1px 6px; border-radius: 8px; line-height: 1.4;
}

.ai-pop-panel {
  position: fixed; right: 24px; bottom: 24px; z-index: 1500;
  width: min(420px, 92vw); height: min(620px, 82vh);
  background: var(--bg-card, #fff); color: var(--text, #1f2937);
  border: 1px solid var(--border, #e5e7eb); border-radius: 16px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.22);
  display: flex; flex-direction: column; overflow: hidden;
}
.ai-pop-enter-active, .ai-pop-leave-active { transition: all 0.22s ease; }
.ai-pop-enter-from, .ai-pop-leave-to { opacity: 0; transform: translateY(20px) scale(0.96); }

.ai-pop-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; border-bottom: 1px solid var(--border, #e5e7eb);
}
.ai-pop-title { display: flex; align-items: center; gap: 10px; }
.ai-pop-logo { font-size: 24px; }
.ai-pop-name { font-size: 15px; font-weight: 700; }
.ai-pop-sub { font-size: 12px; color: var(--text-secondary, #6b7280); margin-top: 1px; }
.ai-pop-actions { display: flex; gap: 4px; }
.ai-pop-icon {
  width: 28px; height: 28px; border-radius: 6px; border: 1px solid var(--border, #e5e7eb);
  background: transparent; color: var(--text-secondary, #6b7280); cursor: pointer; font-size: 13px;
}
.ai-pop-icon:hover:not(:disabled) { background: var(--border-light, #f3f4f6); }
.ai-pop-icon:disabled { opacity: 0.4; cursor: not-allowed; }

.ai-pop-body { flex: 1; overflow-y: auto; padding: 14px; }
.ai-pop-welcome { text-align: center; padding: 24px 8px; }
.ai-pop-welcome-icon { font-size: 40px; }
.ai-pop-welcome-text { font-size: 15px; font-weight: 600; margin-top: 8px; }
.ai-pop-welcome-hint { font-size: 13px; color: var(--text-secondary, #6b7280); margin-top: 4px; }
.ai-pop-suggest { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 16px; }
.ai-pop-chip {
  border: 1px solid var(--border, #e5e7eb); background: var(--border-light, #f3f4f6);
  color: var(--text-secondary, #6b7280); border-radius: 16px; padding: 6px 12px;
  font-size: 12px; cursor: pointer; transition: all 0.15s;
}
.ai-pop-chip:hover { border-color: #7c3aed; color: #7c3aed; }

.ai-pop-msg { display: flex; margin-bottom: 10px; }
.ai-pop-user { justify-content: flex-end; }
.ai-pop-bubble {
  max-width: 86%; padding: 9px 13px; border-radius: 11px;
  font-size: 14px; line-height: 1.65; word-break: break-word;
}
.ai-pop-assistant .ai-pop-bubble {
  background: var(--border-light, #f3f4f6);
  border: 1px solid var(--border, #e5e7eb);
  border-top-left-radius: 4px;
}
.ai-pop-user .ai-pop-bubble {
  background: #7c3aed; color: #fff; border-top-right-radius: 4px;
}
.ai-pop-bubble :deep(.ai-h) { margin: 8px 0 4px; font-size: 14px; font-weight: 700; }
.ai-pop-bubble :deep(.ai-p) { margin: 3px 0; }
.ai-pop-bubble :deep(.ai-ul), .ai-pop-bubble :deep(.ai-ol) { margin: 4px 0; padding-left: 18px; }
.ai-pop-bubble :deep(.ai-ul li), .ai-pop-bubble :deep(.ai-ol li) { margin: 2px 0; }
.ai-pop-bubble :deep(.ai-code) {
  background: #0f172a; color: #e2e8f0; padding: 8px 10px;
  border-radius: 6px; overflow-x: auto; margin: 6px 0; font-size: 12px; line-height: 1.5;
}
.ai-pop-bubble :deep(.ai-inline-code) {
  background: rgba(124, 58, 237, 0.12); color: #7c3aed;
  padding: 1px 4px; border-radius: 3px; font-size: 12px;
}
.ai-pop-user .ai-bubble :deep(.ai-inline-code) { background: rgba(255,255,255,0.2); color: #fff; }

.ai-pop-tip { text-align: center; font-size: 12px; color: var(--text-secondary, #6b7280); padding: 6px 0; }
.ai-pop-err {
  background: rgba(239, 68, 68, 0.08); color: var(--danger, #ef4444);
  border: 1px solid rgba(239, 68, 68, 0.25); border-radius: 6px; padding: 8px 10px; margin-bottom: 8px;
}
.ai-pop-typing { font-size: 13px; color: var(--text-secondary, #6b7280); padding: 4px 0; }
.ai-pop-typing .dot {
  display: inline-block; width: 5px; height: 5px; border-radius: 50%;
  background: #7c3aed; margin: 0 2px; animation: ai-pop-blink 1.2s infinite;
}
.ai-pop-typing .dot:nth-child(2) { animation-delay: 0.2s; }
.ai-pop-typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes ai-pop-blink { 0%, 80%, 100% { opacity: 0.25; } 40% { opacity: 1; } }

.ai-pop-foot {
  display: flex; gap: 8px; align-items: flex-end;
  padding: 10px 14px; border-top: 1px solid var(--border, #e5e7eb);
}
.ai-pop-input {
  flex: 1; resize: none; border: 1px solid var(--border, #e5e7eb); border-radius: 8px;
  padding: 8px 10px; font-size: 14px; background: var(--bg, #f8f9fa); color: var(--text, #1f2937);
  font-family: inherit;
}
.ai-pop-input:focus { outline: none; border-color: #7c3aed; }
.ai-pop-send {
  padding: 8px 16px; border: none; border-radius: 8px; cursor: pointer;
  background: #7c3aed; color: #fff; font-size: 13px; font-weight: 600;
}
.ai-pop-send:disabled { opacity: 0.45; cursor: not-allowed; }
.ai-pop-stop { background: var(--danger, #ef4444); }
</style>
