<template>
  <Teleport to="body">
    <transition name="ai-slide">
      <div v-if="visible" class="ai-mask" @click.self="close">
        <div class="ai-drawer">
          <div class="ai-head">
            <div class="ai-head-title">
              <span class="ai-logo">🤖</span>
              <div>
                <div class="ai-head-name">AI 讲解</div>
                <div class="ai-head-sub" v-if="subTitle">{{ subTitle }}</div>
              </div>
            </div>
            <div class="ai-head-actions">
              <button class="ai-icon-btn" title="重新讲解" @click="startExplain" :disabled="streaming">🔄</button>
              <button class="ai-icon-btn" title="关闭" @click="close">✕</button>
            </div>
          </div>

          <div class="ai-body" ref="bodyRef">
            <div v-if="errorMsg" class="ai-tip ai-tip-error">⚠️ {{ errorMsg }}</div>
            <div v-for="(m, i) in messages" :key="i" class="ai-msg" :class="'ai-msg-' + m.role">
              <div class="ai-bubble" v-html="renderMarkdown(m.content)"></div>
            </div>
            <div v-if="waitingFirst" class="ai-tip ai-typing">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              AI 正在思考…
            </div>
          </div>

          <div class="ai-foot">
            <textarea
              v-model="input"
              class="ai-input"
              rows="2"
              :disabled="streaming || !sessionId"
              :placeholder="sessionId ? '继续追问，如：为什么B选项不对？' : '等待讲解生成…'"
              @keydown.enter.exact.prevent="send"
            ></textarea>
            <button v-if="streaming" class="ai-send ai-send-stop" @click="stop">停止</button>
            <button v-else class="ai-send" @click="send" :disabled="!canSend">发送</button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import api from '../../api'
import { useAiStream } from '../../composables/useAiStream'
import { renderMarkdown } from '../../utils/markdown'

const props = defineProps({
  visible: Boolean,
  /** {id, year, subject, knowledgeTag} 展示信息用 */
  question: Object,
  /** 用户当时的答案（可选） */
  userAnswer: String
})
const emit = defineEmits(['update:visible'])

const { streaming, stream, abort } = useAiStream()

const messages = ref([])
const sessionId = ref(null)
const errorMsg = ref('')
const input = ref('')
const bodyRef = ref(null)

// 组件实例级缓存：questionId -> sessionId，重复打开不重复计费
const sessionCache = new Map()
let currentQid = null

const subTitle = computed(() => {
  const q = props.question || {}
  const parts = []
  if (q.year) parts.push(q.year + '年')
  if (q.subject) parts.push(q.subject)
  if (q.knowledgeTag) parts.push(q.knowledgeTag)
  return parts.join(' · ')
})

const canSend = computed(() => !!input.value.trim() && !!sessionId.value)
const waitingFirst = computed(() => {
  if (!streaming.value) return false
  const last = messages.value[messages.value.length - 1]
  return last && last.role === 'assistant' && !last.content
})

watch(() => props.visible, (v) => {
  if (v && props.question && props.question.id) init()
})

watch(() => props.question && props.question.id, () => {
  // 抽屉开着时切换题目（如列表模式）则重新初始化
  if (props.visible && props.question && props.question.id) init()
})

async function init() {
  currentQid = props.question.id
  messages.value = []
  errorMsg.value = ''
  input.value = ''
  sessionId.value = null
  scrollBottom()

  const cached = sessionCache.get(currentQid)
  if (cached) {
    sessionId.value = cached
    try {
      const r = await api.get(`/ai/sessions/${cached}/messages`)
      if (r.data.code === 200 && Array.isArray(r.data.data) && r.data.data.length) {
        messages.value = r.data.data.map(m => ({ role: m.role, content: m.content }))
        scrollBottom()
        return
      }
    } catch (e) { /* 会话失效则重新讲解 */ }
  }
  startExplain()
}

function startExplain() {
  if (!props.question || !props.question.id || streaming.value) return
  messages.value = []
  errorMsg.value = ''
  messages.value.push({ role: 'assistant', content: '' })
  scrollBottom()
  stream({
    url: '/ai/explain/stream',
    body: { questionId: props.question.id, userAnswer: props.userAnswer || '' },
    onEvent: handleEvent,
    onError: handleError
  })
}

function send() {
  const text = input.value.trim()
  if (!text || !sessionId.value || streaming.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: text })
  messages.value.push({ role: 'assistant', content: '' })
  scrollBottom()
  stream({
    url: '/ai/chat/stream',
    body: { sessionId: sessionId.value, message: text },
    onEvent: handleEvent,
    onError: handleError
  })
}

function handleEvent(evt) {
  if (evt.type === 'meta') {
    sessionId.value = evt.sessionId
    if (currentQid) sessionCache.set(currentQid, evt.sessionId)
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
function close() { emit('update:visible', false) }

function scrollBottom() {
  nextTick(() => {
    if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
  })
}
</script>

<style scoped>
.ai-mask {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0, 0, 0, 0.35);
}
.ai-drawer {
  position: absolute; top: 0; right: 0; bottom: 0;
  width: min(480px, 94vw);
  background: var(--bg-card, #fff);
  color: var(--text, #1f2937);
  border-left: 1px solid var(--border, #e5e7eb);
  box-shadow: -8px 0 30px rgba(0, 0, 0, 0.18);
  display: flex; flex-direction: column;
}
.ai-slide-enter-active, .ai-slide-leave-active { transition: all 0.25s ease; }
.ai-slide-enter-from, .ai-slide-leave-to { opacity: 0; }
.ai-slide-enter-from .ai-drawer, .ai-slide-leave-to .ai-drawer { transform: translateX(40px); }

.ai-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px; border-bottom: 1px solid var(--border, #e5e7eb);
}
.ai-head-title { display: flex; align-items: center; gap: 10px; }
.ai-logo { font-size: 26px; }
.ai-head-name { font-size: 16px; font-weight: 700; }
.ai-head-sub { font-size: 12px; color: var(--text-secondary, #6b7280); margin-top: 2px; }
.ai-head-actions { display: flex; gap: 6px; }
.ai-icon-btn {
  width: 30px; height: 30px; border-radius: 8px; border: 1px solid var(--border, #e5e7eb);
  background: transparent; color: var(--text-secondary, #6b7280); cursor: pointer; font-size: 14px;
}
.ai-icon-btn:hover:not(:disabled) { background: var(--border-light, #f3f4f6); }
.ai-icon-btn:disabled { opacity: 0.4; cursor: not-allowed; }

.ai-body { flex: 1; overflow-y: auto; padding: 16px; }
.ai-msg { display: flex; margin-bottom: 12px; }
.ai-msg-user { justify-content: flex-end; }
.ai-bubble {
  max-width: 88%; padding: 10px 14px; border-radius: 12px;
  font-size: 14px; line-height: 1.7; word-break: break-word;
}
.ai-msg-assistant .ai-bubble {
  background: var(--border-light, #f3f4f6);
  border: 1px solid var(--border, #e5e7eb);
  border-top-left-radius: 4px;
}
.ai-msg-user .ai-bubble {
  background: var(--primary, #059669); color: #fff;
  border-top-right-radius: 4px;
}
/* markdown 内容样式 */
.ai-bubble :deep(.ai-h) { margin: 10px 0 6px; font-size: 15px; font-weight: 700; }
.ai-bubble :deep(.ai-p) { margin: 4px 0; }
.ai-bubble :deep(.ai-ul), .ai-bubble :deep(.ai-ol) { margin: 4px 0; padding-left: 20px; }
.ai-bubble :deep(.ai-ul li), .ai-bubble :deep(.ai-ol li) { margin: 2px 0; }
.ai-bubble :deep(.ai-code) {
  background: #0f172a; color: #e2e8f0; padding: 10px 12px;
  border-radius: 8px; overflow-x: auto; margin: 8px 0; font-size: 13px; line-height: 1.5;
}
.ai-bubble :deep(.ai-inline-code) {
  background: rgba(5, 150, 105, 0.12); color: #059669;
  padding: 1px 5px; border-radius: 4px; font-size: 13px;
}
.ai-msg-user .ai-bubble :deep(.ai-inline-code) { background: rgba(255,255,255,0.2); color: #fff; }

.ai-tip {
  text-align: center; font-size: 13px; color: var(--text-secondary, #6b7280); padding: 8px 0;
}
.ai-tip-error {
  background: rgba(239, 68, 68, 0.08); color: var(--danger, #ef4444);
  border: 1px solid rgba(239, 68, 68, 0.25); border-radius: 8px; padding: 10px 12px; margin-bottom: 10px;
}
.ai-typing .dot {
  display: inline-block; width: 5px; height: 5px; border-radius: 50%;
  background: var(--primary, #059669); margin: 0 2px; animation: ai-blink 1.2s infinite;
}
.ai-typing .dot:nth-child(2) { animation-delay: 0.2s; }
.ai-typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes ai-blink { 0%, 80%, 100% { opacity: 0.25; } 40% { opacity: 1; } }

.ai-foot {
  display: flex; gap: 8px; align-items: flex-end;
  padding: 12px 16px; border-top: 1px solid var(--border, #e5e7eb);
}
.ai-input {
  flex: 1; resize: none; border: 1px solid var(--border, #e5e7eb); border-radius: 10px;
  padding: 8px 12px; font-size: 14px; background: var(--bg, #f8f9fa); color: var(--text, #1f2937);
  font-family: inherit;
}
.ai-input:focus { outline: none; border-color: var(--primary, #059669); }
.ai-send {
  padding: 9px 18px; border: none; border-radius: 10px; cursor: pointer;
  background: var(--primary, #059669); color: #fff; font-size: 14px; font-weight: 600;
}
.ai-send:disabled { opacity: 0.45; cursor: not-allowed; }
.ai-send-stop { background: var(--danger, #ef4444); }
</style>
