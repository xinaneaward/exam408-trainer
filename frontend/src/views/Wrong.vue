<template>
  <!-- 错题重练作答阶段 -->
  <div v-if="phase === 'review'" class="page-root" :class="theme">
    <TopNav :username="user?.nickname||'用户'" @toggle-theme="toggleTheme" @logout="logout" />
    <div class="page-body">
      <main class="page-content">
        <div class="page-header">
          <h2>错题重练 <span style="font-size:14px;color:var(--text-secondary);font-weight:400;">{{ reviewIndex + 1 }}/{{ paperQuestions.length }}</span></h2>
          <div style="display:flex;gap:8px;">
            <button class="btn btn-outline btn-sm" @click="exitReview">退出</button>
            <button class="btn btn-primary btn-sm" @click="startReview" :disabled="paperLoading">{{ paperLoading ? '加载中...' : '✏️ 错题重练' }}</button>
            <button class="btn btn-outline btn-sm" @click="goBack">← 返回</button>
          </div>
        </div>

        <div class="exam-topbar" style="display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-bottom:16px;">
          <span class="exam-timer">⏱ {{ formatTime(timerSeconds) }}</span>
          <span style="font-size:13px;color:var(--text-secondary);">{{ answeredCount }}/{{ paperQuestions.length }} 已答</span>
          <div class="progress-bar" style="flex:1;min-width:120px;">
            <div class="progress-fill" :style="{ width: answeredPercent + '%' }"></div>
          </div>
        </div>

        <div v-if="curReviewQ" class="question-item">
          <div class="question-header">
            <span class="question-badge">{{ curReviewQ.subject }}</span>
            <span class="question-badge" style="background:#fce8e6;color:var(--danger);">{{ curReviewQ.type }}</span>
            <span class="question-badge">{{ curReviewQ.examYear || curReviewQ.year }}年</span>
            <span v-if="curReviewQ.knowledgeTag" class="question-badge" style="background:#fef7e0;color:#e37400;">{{ curReviewQ.knowledgeTag }}</span>
            <span v-if="curReviewQ.wrongCount" class="question-badge" style="background:#fdecea;color:var(--danger);">曾错{{ curReviewQ.wrongCount }}次</span>
          </div>
          <div class="question-content" v-html="renderContent(curReviewQ.content)"></div>

          <ul v-if="curReviewQ.type === '单选' && parseOptions(curReviewQ.options).length" class="options-list">
            <li
              v-for="opt in parseOptions(curReviewQ.options)" :key="opt.key"
              class="option-item" :class="{ selected: reviewAnswers[curReviewQ.id] === opt.key }"
              @click="reviewAnswers[curReviewQ.id] = opt.key"
            >
              <span class="option-key">{{ opt.key }}</span>
              <span>{{ opt.text }}</span>
            </li>
          </ul>

          <div v-else-if="curReviewQ.type === '综合应用'" style="margin-bottom:14px;">
            <div class="analysis-box">
              <div style="color:#1565c0;font-weight:600;font-size:14px;">💡 参考答案</div>
              <span class="an-item"><strong>答案：</strong>{{ curReviewQ.answer || '暂无答案' }}</span>
              <span v-if="curReviewQ.analysis" class="an-item"><strong>解析：</strong>{{ formatAnalysis(curReviewQ.analysis) }}</span>
              <span v-else class="an-item" style="color:var(--text-secondary);">暂无解析</span>
            </div>
          </div>

          <div class="se-nav-buttons" style="display:flex;gap:12px;justify-content:space-between;margin-top:16px;flex-wrap:wrap;">
            <button class="btn btn-outline" :disabled="reviewIndex === 0" @click="reviewIndex--">← 上一题</button>
            <button v-if="reviewIndex < paperQuestions.length - 1" class="btn btn-outline" @click="reviewIndex++">下一题 →</button>
            <button v-else class="btn btn-primary" @click="submitReviewPaper" :disabled="submitting">{{ submitting ? '提交中...' : '完成并提交' }}</button>
          </div>
        </div>
      </main>
    </div>
  </div>

  <!-- 错题重练结果 -->
  <div v-else-if="phase === 'reviewResult'" class="page-root" :class="theme">
    <TopNav :username="user?.nickname||'用户'" @toggle-theme="toggleTheme" @logout="logout" />
    <div class="page-body">
      <main class="page-content">
        <div class="page-header"><h2>重练结果</h2></div>

        <div class="result-summary" style="display:flex;gap:16px;flex-wrap:wrap;margin-bottom:20px;">
          <div class="stat-card"><div class="stat-value" style="font-size:24px;">{{ resultStats.correct }}</div><div class="stat-label">✓ 做对</div></div>
          <div class="stat-card"><div class="stat-value" style="font-size:24px;color:var(--danger);">{{ resultStats.wrong }}</div><div class="stat-label">✗ 仍错</div></div>
          <div class="stat-card"><div class="stat-value" style="font-size:24px;color:#f57c00;">{{ resultStats.unanswered }}</div><div class="stat-label">○ 未答</div></div>
          <div class="stat-card"><div class="stat-value" style="font-size:24px;">{{ resultStats.accuracy }}%</div><div class="stat-label">正确率</div></div>
          <div class="stat-card"><div class="stat-value" style="font-size:24px;">{{ formatTime(resultStats.duration) }}</div><div class="stat-label">用时</div></div>
        </div>

        <div v-if="knowledgeBreakdown.length > 0" class="result-analysis">
          <h3 style="margin:0 0 12px 0;font-size:17px;color:var(--text);">📊 知识点分布（薄弱点优先）</h3>
          <div v-for="kb in knowledgeBreakdown" :key="kb.tag" class="subject-breakdown" style="margin-bottom:12px;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
              <span style="font-weight:600;">{{ kb.tag || '未标注知识点' }}</span>
              <span style="color:var(--text-secondary);font-size:13px;">{{ kb.correct }}/{{ kb.total }} 题 · 正确率 {{ kb.accuracy }}%</span>
            </div>
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: kb.accuracy + '%', background: kb.accuracy >= 70 ? 'var(--success)' : kb.accuracy >= 50 ? '#f57c00' : 'var(--danger)' }"></div>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;margin-top:4px;font-size:12px;color:var(--text-secondary);">
              <span>掌握 {{ kb.correct }} 题 · 仍错 {{ kb.wrong }} 题</span>
              <button class="btn btn-sm" style="background:#059669;color:#fff;border:none;" @click="goKnowledgePractice(kb)">📌 生成该知识点练习</button>
            </div>
          </div>
        </div>

        <div v-if="wrongHere.length > 0" class="result-analysis">
          <h3 style="margin:0 0 12px 0;font-size:17px;color:var(--text);">📝 仍错题目回顾</h3>
          <div v-for="(d, i) in wrongHere" :key="i" class="result-detail wrong-detail" style="background:var(--bg-card);border:1px solid var(--border);border-radius:8px;padding:14px;margin-bottom:12px;">
            <div style="display:flex;align-items:flex-start;gap:10px;">
              <div style="flex:1;min-width:0;">
                <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                  <span style="font-size:12px;color:var(--text-secondary);">{{ d.subject }} · {{ d.type }} <span v-if="d.knowledgeTag" style="margin-left:6px;">· {{ d.knowledgeTag }}</span></span>
                </div>
                <div style="font-size:14px;line-height:1.6;margin-bottom:8px;" v-html="d.content"></div>
                <div style="font-size:13px;line-height:1.8;">
                  <div>你的答案：<span style="color:var(--danger);font-weight:600;">{{ d.userAnswer || '未作答' }}</span></div>
                  <div>正确答案：<span style="color:var(--success);font-weight:600;">{{ d.answer }}</span></div>
                  <div v-if="d.analysis" style="margin-top:6px;color:var(--text-secondary);border-left:3px solid var(--border);padding-left:10px;">💡 {{ d.analysis }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div style="display:flex;gap:12px;justify-content:center;margin-top:24px;flex-wrap:wrap;">
          <button class="btn btn-primary" @click="startReview()">再做一组</button>
          <button class="btn btn-outline" @click="exitReview">返回错题本</button>
        </div>
      </main>
    </div>
  </div>

  <!-- 错题本默认列表 -->
  <div v-else class="page-root" :class="theme">
    <TopNav :username="user?.nickname||'用户'" @toggle-theme="toggleTheme" @logout="logout" />
    <div class="page-body">
      <main class="page-content">
      <div class="page-header">
        <h2>错题本</h2>
        <div class="page-header-actions">
          <button class="btn btn-primary btn-sm" @click="startReview" :disabled="paperLoading">{{ paperLoading ? '加载中...' : '✏️ 错题重练' }}</button>
        </div>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">{{ wrongCountData.total || 0 }}</div>
          <div class="stat-label">错题总数</div>
        </div>
        <div class="stat-card" v-for="s in subjectList" :key="s.key">
          <div class="stat-value" style="font-size:22px;">{{ wrongCountData.bySubject?.[s.label] || 0 }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </div>

      <div class="card" v-if="reasonStats.total > 0">
        <div class="card-title">错题原因分布</div>
        <div class="reason-summary" style="font-size:13px; color:var(--text-secondary); margin-bottom:12px;">
          已标注原因 {{ reasonStats.tagged }} / {{ reasonStats.total }} 题（未标注 {{ reasonStats.untagged }} 题，请在列表下方打标）
        </div>
        <div v-for="r in reasonOptions" :key="r" style="margin-bottom:12px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
            <span style="font-size:14px; font-weight:500;">{{ r }}</span>
            <span style="font-size:13px; color:var(--text-secondary);">{{ reasonStats.counts?.[r] || 0 }} 题 · {{ reasonPercent(r) }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: reasonPercent(r) + '%' }"></div>
          </div>
        </div>
      </div>

      <div class="wrong-filter">
        <label class="form-label" style="margin-bottom:0; display:flex; align-items:center; gap:8px;">
          科目筛选：
          <select class="form-input" v-model="filterSubject" @change="loadWrongList">
            <option value="">全部</option>
            <option v-for="s in subjectList" :key="s.key" :value="s.label">{{ s.label }}</option>
          </select>
        </label>
      </div>

      <div v-if="wrongItems.length === 0" style="text-align:center; color:var(--text-secondary); padding:40px 0;">
        暂无错题记录
      </div>

      <div v-for="item in wrongItems" :key="item.wrongId" class="question-item wrong">
        <div class="question-header">
          <div style="display:flex; align-items:center; gap:8px; flex-wrap:wrap;">
            <span class="question-badge">{{ item.subject }}</span>
            <span class="question-badge" style="background:#fce8e6; color:var(--danger);">{{ item.type }}</span>
            <span class="question-badge">{{ item.year }}年</span>
            <span v-if="item.knowledgeTag" class="question-badge" style="background:#fef7e0; color:#e37400;">{{ item.knowledgeTag }}</span>
            <span v-if="item.wrongCount > 0" style="font-size:12px; color:var(--danger);">
              错{{ item.wrongCount }}次
            </span>
            <span v-if="isDue(item)" class="question-badge" style="background:#f57c00; color:#fff;">⏰ 待复习</span>
            <span v-else-if="item.isReviewed" class="question-badge" style="background:#e8f5e9; color:var(--success);">已掌握</span>
          </div>
          <div style="display:flex; gap:8px;">
            <button class="btn btn-success btn-sm" @click="markAsReviewed(item.wrongId)" :disabled="item.isReviewed">
              {{ item.isReviewed ? '已掌握' : '标记掌握' }}
            </button>
            <button class="btn btn-danger btn-sm" @click="removeItem(item.wrongId)">删除</button>
          </div>
        </div>

        <div class="question-content" v-html="renderContent(item.content)"></div>

        <div class="reason-row">
          <span class="reason-label">错误原因：</span>
          <button
            v-for="r in reasonOptions" :key="r"
            class="reason-pill" :class="{ active: item.reason === r }"
            @click="setReason(item, r)"
          >{{ r }}</button>
          <button v-if="item.reason" class="reason-pill clear" @click="setReason(item, '')">清除</button>
        </div>

        <div v-if="parsePageImg(item.content)" style="margin-bottom:12px;">
          <button class="btn btn-outline btn-sm" @click="viewPageImg(parsePageImg(item.content))">📄 查看原题</button>
        </div>

        <div class="analysis-box">
          <div class="analysis-title">📖 答案与解析</div>
          <span v-if="item.answer" class="an-item"><strong>答案：</strong>{{ item.answer }}</span>
          <span v-if="item.analysis" class="an-item"><strong>解析：</strong>{{ formatAnalysis(item.analysis) }}</span>
        </div>
      </div>
    </main>
    </div>

    <!-- AI 讲解抽屉 -->
    <AiChatPanel v-model="aiPanel.visible" :question="aiPanel.question" :user-answer="aiPanel.userAnswer" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import TopNav from '../components/TopNav.vue'
import AiChatPanel from '../components/ai/AiChatPanel.vue'
import { useAiContextStore } from '../stores/aiContext'
import { formatQuestionContent, formatAnalysis } from '../utils/questionContent'

const router = useRouter()
const aiCtx = useAiContextStore()

const aiPanel = ref({ visible: false, question: null, userAnswer: '' })
const openAi = (item) => {
  if (!item.questionId) return
  const q = { id: item.questionId, year: item.year, subject: item.subject, knowledgeTag: item.knowledgeTag }
  aiCtx.setQuestion(q)
  aiPanel.value = { visible: true, question: q, userAnswer: item.userAnswer || '' }
}

const openAiQ = (d) => {
  if (!d || !d.qid) return
  const q = { id: d.qid, subject: d.subject, knowledgeTag: d.knowledgeTag }
  aiCtx.setQuestion(q)
  aiPanel.value = { visible: true, question: q, userAnswer: d.userAnswer || '' }
}

const goKnowledgePractice = (kb) => {
  if (!kb || !kb.tag) return
  router.push({ path: '/practice', query: { subject: kb.subject || '', knowledgeTag: kb.tag } })
}

const goBack = () => {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

const user = ref(null)
const wrongItems = ref([])
const wrongCountData = ref({ total: 0, bySubject: {} })
const reasonStats = ref({ total: 0, tagged: 0, untagged: 0, counts: {} })
const reasonOptions = ['概念不清', '粗心', '计算错', '审题不清', '其他']
const filterSubject = ref('')
const theme = ref(localStorage.getItem('theme') || 'light')

const phase = ref('list')
const paperQuestions = ref([])
const reviewAnswers = ref({})
const reviewIndex = ref(0)
const timerSeconds = ref(0)
const submitting = ref(false)
const paperLoading = ref(false)
const reviewResult = ref(null)
let timerInterval = null

const subjectList = [
  { key: 'ds', label: '数据结构' },
  { key: 'co', label: '计算机组成原理' },
  { key: 'os', label: '操作系统' },
  { key: 'cn', label: '计算机网络' }
]

const parsePageImg = (content) => {
  if (!content) return null
  const match = content.match(/<!--\s*page_img:(.*?)\s*-->/)
  return match ? match[1].trim() : null
}

const renderContent = (content) => formatQuestionContent(content)

const viewPageImg = (imgPath) => {
  window.open(imgPath, '_blank')
}

const parseOptions = (options) => {
  if (!options) return []
  try {
    const arr = typeof options === 'string' ? JSON.parse(options) : options
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

const curReviewQ = computed(() => paperQuestions.value[reviewIndex.value] || null)
const answeredCount = computed(() => paperQuestions.value.filter(q => reviewAnswers.value[q.id]).length)
const answeredPercent = computed(() => paperQuestions.value.length ? Math.round(answeredCount.value / paperQuestions.value.length * 100) : 0)

const formatTime = (s) => {
  s = s || 0
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2,'0')}:${String(sec).padStart(2,'0')}`
}

const getScore = (q) => (q.type === '综合应用' ? 9 : 2)

const startReview = async () => {
  if (paperLoading.value) return
  paperLoading.value = true
  try {
    const params = filterSubject.value ? { subject: filterSubject.value } : {}
    const r = await api.getWrongPaper(params)
    if (r.data.code === 200 && r.data.data && r.data.data.length > 0) {
      paperQuestions.value = r.data.data
      reviewAnswers.value = {}
      reviewIndex.value = 0
      timerSeconds.value = 0
      reviewResult.value = null
      phase.value = 'review'
      startTimer()
    } else {
      alert('当前筛选下没有可重练的错题')
    }
  } catch (e) {
    alert('加载重练题目失败')
  } finally {
    paperLoading.value = false
  }
}

const submitReviewPaper = async () => {
  if (submitting.value) return
  submitting.value = true
  stopTimer()
  try {
    // 综合应用题仅展示答案，只提交单选题
    const answers = paperQuestions.value
      .filter(q => q.type === '单选')
      .map(q => ({
        questionId: q.id,
        userAnswer: reviewAnswers.value[q.id] || ''
      }))
    const response = await api.submitAnswers({ mode: 'practice', subject: filterSubject.value, answers, durationSeconds: timerSeconds.value })
    if (response.data && response.data.data) {
      const srvMap = {}
      for (const s of (response.data.data.questions || [])) srvMap[s.questionId] = s
      let correct = 0, wrong = 0, unanswered = 0
      const tagMap = {}
      const wrongDetails = []
      for (const q of paperQuestions.value) {
        if (q.type !== '单选') continue
        const ua = reviewAnswers.value[q.id] || ''
        const srv = srvMap[q.id]
        const earned = srv && srv.score != null ? srv.score : (ua && ua === q.answer ? getScore(q) : 0)
        const isCorrect = srv ? !!srv.isCorrect : (!!ua && ua === q.answer)
        if (!ua) unanswered++
        else if (isCorrect) correct++
        else wrong++

        const tag = q.knowledgeTag || ''
        if (!tagMap[tag]) tagMap[tag] = { total: 0, correct: 0, wrong: 0, subject: '' }
        tagMap[tag].total++
        if (isCorrect) tagMap[tag].correct++
        else if (ua) tagMap[tag].wrong++
        if (!tagMap[tag].subject) tagMap[tag].subject = q.subject

        // 间隔重复：做对推进阶段、做错重置（错题由提交接口自动再次收录）
        if (q.wrongId) {
          api.recordReviewResult(q.wrongId, isCorrect).catch(() => {})
        }

        if (!isCorrect && ua) {
          wrongDetails.push({
            qid: q.id, subject: q.subject, type: q.type, knowledgeTag: tag,
            content: renderContent(q.content), userAnswer: ua, answer: q.answer,
            analysis: q.analysis
          })
        }
      }
      const total = paperQuestions.value.filter(q => q.type === '单选').length || 0
      const accuracy = total ? Math.round((correct / total) * 100) : 0
      const knowledgeBreakdown = Object.keys(tagMap).map(tag => ({
        tag,
        subject: tagMap[tag].subject,
        total: tagMap[tag].total,
        correct: tagMap[tag].correct,
        wrong: tagMap[tag].wrong,
        accuracy: tagMap[tag].total ? Math.round((tagMap[tag].correct / tagMap[tag].total) * 100) : 0
      })).sort((a, b) => a.accuracy - b.accuracy)
      reviewResult.value = {
        correct, wrong, unanswered, accuracy, duration: timerSeconds.value,
        knowledgeBreakdown, wrongDetails
      }
      if (wrongDetails.length > 0) {
        await api.getWrongList().catch(() => {})
        loadWrongList(); loadWrongCount()
      }
      phase.value = 'reviewResult'
    }
  } catch (e) {
    alert('提交失败，请重试')
    startTimer()
  } finally {
    submitting.value = false
  }
}

const exitReview = () => {
  stopTimer()
  phase.value = 'list'
  paperQuestions.value = []
  reviewAnswers.value = {}
  reviewResult.value = null
  loadWrongList()
  loadWrongCount()
  loadReasonStats()
}

const startTimer = () => {
  stopTimer()
  timerInterval = setInterval(() => { timerSeconds.value++ }, 1000)
}
const stopTimer = () => {
  if (timerInterval) { clearInterval(timerInterval); timerInterval = null }
}

const resultStats = computed(() => reviewResult.value || { correct: 0, wrong: 0, unanswered: 0, accuracy: 0, duration: 0 })
const knowledgeBreakdown = computed(() => reviewResult.value?.knowledgeBreakdown || [])
const wrongHere = computed(() => reviewResult.value?.wrongDetails || [])

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('theme', theme.value)
}

const loadWrongList = async () => {
  try {
    const params = filterSubject.value ? { subject: filterSubject.value } : {}
    const r = await api.getWrongList(params)
    if (r.data.code === 200) {
      wrongItems.value = r.data.data || []
    }
  } catch (e) { }
}

const loadWrongCount = async () => {
  try {
    const r = await api.getWrongCount()
    if (r.data.code === 200) {
      wrongCountData.value = r.data.data || { total: 0, bySubject: {} }
    }
  } catch (e) { }
}

const loadReasonStats = async () => {
  try {
    const r = await api.getReasonStats()
    if (r.data.code === 200) {
      reasonStats.value = r.data.data || { total: 0, tagged: 0, untagged: 0, counts: {} }
    }
  } catch (e) { }
}

const reasonPercent = (r) => {
  const total = reasonStats.value.total || 0
  if (!total) return 0
  return Math.round((reasonStats.value.counts?.[r] || 0) / total * 100)
}

const setReason = async (item, reason) => {
  try {
    const r = await api.setWrongReason(item.wrongId, reason)
    if (r.data.code === 200) {
      item.reason = reason || null
      loadReasonStats()
    }
  } catch (e) { }
}

const isDue = (item) => {
  if (item.isReviewed) return false
  if (item.nextReviewAt == null) return true
  return new Date(item.nextReviewAt) <= new Date()
}

const markAsReviewed = async (id) => {
  try {
    const r = await api.markReviewed(id)
    if (r.data.code === 200) {
      const item = wrongItems.value.find(i => i.wrongId === id)
      if (item) item.isReviewed = true
    }
  } catch (e) { }
}

const removeItem = async (id) => {
  try {
    const r = await api.removeWrong(id)
    if (r.data.code === 200) {
      wrongItems.value = wrongItems.value.filter(i => i.wrongId !== id)
      loadWrongCount()
      loadReasonStats()
    }
  } catch (e) { }
}

const logout = async () => {
  try { await api.logout() } catch (e) { }
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  const raw = localStorage.getItem('user')
  user.value = raw ? JSON.parse(raw) : null
  loadWrongList()
  loadWrongCount()
  loadReasonStats()
})

onBeforeUnmount(() => {
  stopTimer()
})
</script>

<style scoped>
.page-root { min-height:100vh; }
.page-body { display:flex; }
.page-content { flex:1; padding:20px; }
.page-header { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; }
.exam-topbar { border:1px solid var(--border); border-radius:12px; padding:12px 16px; }
.se-nav-buttons button { min-width:110px; }

/* 错题原因打标 */
.reason-row {
  display:flex; align-items:center; flex-wrap:wrap; gap:6px;
  margin-bottom:12px; padding:8px 0;
}
.reason-label { font-size:13px; color:var(--text-secondary); margin-right:2px; }
.reason-pill {
  padding:3px 12px; border-radius:14px; border:1px solid var(--border);
  background:var(--bg-card); color:var(--text-secondary);
  font-size:12px; cursor:pointer; transition:all 0.2s;
}
.reason-pill:hover { border-color:var(--primary); color:var(--primary); }
.reason-pill.active {
  background:rgba(5,150,105,0.15); border-color:var(--primary);
  color:var(--success); font-weight:600;
}
.reason-pill.clear { border-style:dashed; }
</style>