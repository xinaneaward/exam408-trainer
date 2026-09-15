<template>
  <div class="se-root">
    <TopNav :username="user?.nickname||'用户'" @toggle-theme="toggleTheme" @logout="logout" />

    <div class="se-body">
      <template v-if="phase === 'config'">
        <div class="se-header">
          <h1 class="se-title">智能组卷</h1>
          <p class="se-desc">根据学习数据智能选题，精准定位薄弱环节，定制专属试卷</p>
        </div>

        <div class="se-config-grid">
          <div class="se-config-card">
            <div class="se-config-title">选择科目</div>
            <div class="se-subject-grid">
              <button
                v-for="s in subjects" :key="s.key"
                class="se-subject-btn"
                :class="{ active: config.subjects.includes(s.key) }"
                @click="toggleSubject(s.key)"
              >
                <span class="se-subj-icon">{{ s.icon }}</span>
                <span>{{ s.name }}</span>
              </button>
            </div>
          </div>

          <div class="se-config-card">
            <div class="se-config-title">题目数量</div>
            <div class="se-slider-row">
              <input type="range" min="5" max="40" v-model.number="config.count" class="se-slider" />
              <span class="se-slider-val">{{ config.count }}题</span>
            </div>
          </div>

          <div class="se-config-card">
            <div class="se-config-title">难度偏好</div>
            <div class="se-difficulty-row">
              <button
                v-for="d in difficulties" :key="d.key"
                class="se-diff-btn"
                :class="{ active: config.difficulty === d.key }"
                @click="config.difficulty = d.key"
              >{{ d.label }}</button>
            </div>
          </div>

          <div class="se-config-card">
            <div class="se-config-title">题型选择</div>
            <div class="se-type-row">
              <label class="se-check-label">
                <input type="checkbox" v-model="config.types" value="单选" />
                <span>单选题</span>
              </label>
              <label class="se-check-label">
                <input type="checkbox" v-model="config.types" value="综合应用" />
                <span>综合应用题</span>
              </label>
            </div>
          </div>

          <div class="se-config-card">
            <div class="se-config-title">年份范围</div>
            <div class="se-year-row">
              <select class="se-select" v-model.number="config.yearStart">
                <option v-for="y in allYears" :key="y" :value="y">{{ y }}</option>
              </select>
              <span>至</span>
              <select class="se-select" v-model.number="config.yearEnd">
                <option v-for="y in allYears" :key="y" :value="y">{{ y }}</option>
              </select>
            </div>
          </div>

          <div class="se-config-card">
            <div class="se-config-title">知识点</div>
            <div class="se-knowledge-row">
              <select class="se-select" v-model="config.knowledgeTag" @change="onKnowledgeTagChange">
                <option value="">全部知识点</option>
                <option v-for="tag in knowledgeTags" :key="tag" :value="tag">{{ tag }}</option>
              </select>
            </div>
          </div>

          <div class="se-config-card se-summary-card">
            <div class="se-config-title">试卷摘要</div>
            <div class="se-summary-items">
              <div class="se-summary-item">
                <span>科目</span><strong>{{ config.subjects.length > 0 ? config.subjects.join('、') : '全部' }}</strong>
              </div>
              <div class="se-summary-item">
                <span>题数</span><strong>{{ config.count }}题</strong>
              </div>
              <div class="se-summary-item">
                <span>难度</span><strong>{{ difficulties.find(d => d.key === config.difficulty)?.label }}</strong>
              </div>
              <div class="se-summary-item">
                <span>年份</span><strong>{{ config.yearStart }}-{{ config.yearEnd }}</strong>
              </div>
              <div class="se-summary-item">
                <span>知识点</span><strong>{{ config.knowledgeTag || '全部' }}</strong>
              </div>
            </div>
            <button class="se-generate-btn" @click="generateExam" :disabled="generating">
              {{ generating ? '生成中...' : '开始组卷' }}
            </button>
          </div>
        </div>
      </template>

      <template v-if="phase === 'exam'">
        <div class="exam-topbar">
          <span class="exam-timer">{{ formatTimer(timerSeconds) }}</span>
          <span class="exam-progress">{{ examIndex + 1 }}/{{ examQuestions.length }}</span>
          <div class="progress-bar" style="width:120px;">
            <div class="progress-fill" :style="{ width: examProgressPercent + '%' }"></div>
          </div>
          <button class="btn btn-outline btn-sm" style="margin-left:auto;" @click="exportCurrentPdf">📄 导出PDF</button>
          <button class="btn btn-outline btn-sm" style="color:#dc2626;border-color:#dc2626;" @click="exitSmartExam">退出考试</button>
          <button class="btn btn-primary btn-sm" @click="submitExam">提交试卷</button>
          <button class="btn btn-outline btn-sm" @click="goBack">← 返回</button>
        </div>

        <div v-if="curExamQ" class="question-item">
          <div class="question-header">
            <span class="question-badge">{{ curExamQ.subject }}</span>
            <span class="question-badge" style="background:#e8f5e9;color:#2e7d32;">{{ curExamQ.year || curExamQ.examYear }}年</span>
            <span class="question-badge" style="background:#fce4ec;color:#c62828;">{{ curExamQ.type }}</span>
            <span class="question-number" style="margin-left:auto;">{{ examIndex + 1 }}/{{ examQuestions.length }}</span>
          </div>
          <div class="question-content" v-html="formatQuestionContent(curExamQ.content)"></div>

          <ul v-if="curExamQ.type === '单选' && parseOptions(curExamQ.options).length" class="options-list">
            <li
              v-for="opt in parseOptions(curExamQ.options)" :key="opt.key"
              class="option-item"
              :class="{ selected: examAnswers[curExamQ.id] === opt.key }"
              @click="examAnswers[curExamQ.id] = opt.key"
            >
              <span class="option-key">{{ opt.key }}</span>
              <span>{{ opt.text }}</span>
            </li>
          </ul>

          <div v-if="curExamQ.type === '综合应用'" style="margin-bottom:14px;">
            <div class="analysis-box">
              <div style="color:#1565c0;font-weight:600;font-size:14px;">💡 参考答案</div>
              <span class="an-item"><strong>答案：</strong>{{ curExamQ.answer || '暂无答案' }}</span>
              <span v-if="curExamQ.analysis" class="an-item"><strong>解析：</strong>{{ formatAnalysis(curExamQ.analysis) }}</span>
              <span v-else class="an-item" style="color:var(--text-secondary);">暂无解析</span>
            </div>
          </div>

          <div class="se-nav-buttons">
            <button class="btn btn-outline" :disabled="examIndex === 0" @click="examIndex--">← 上一题</button>
            <span style="font-size:13px;color:var(--text-secondary);">{{ examAnsweredCount }}/{{ singleCount }} 已答</span>
            <button v-if="examIndex < examQuestions.length - 1" class="btn btn-outline" @click="examIndex++">下一题 →</button>
            <button v-else class="btn btn-primary" @click="submitExam" :disabled="submitting">{{ submitting ? '提交中...' : '提交' }}</button>
          </div>
        </div>
      </template>

      <template v-if="phase === 'result'">
        <div class="se-header">
          <h1 class="se-title">考试结果</h1>
        </div>
        <div class="result-summary">
          <div class="result-item"><div class="result-num blue">{{ result.total }}</div><div style="font-size:12px;color:var(--text-secondary);">总题数</div></div>
          <div class="result-item"><div class="result-num green">{{ result.correct }}</div><div style="font-size:12px;color:var(--text-secondary);">正确</div></div>
          <div class="result-item"><div class="result-num red">{{ result.wrong }}</div><div style="font-size:12px;color:var(--text-secondary);">错误</div></div>
          <div class="result-item"><div class="result-num blue">{{ result.accuracy }}%</div><div style="font-size:12px;color:var(--text-secondary);">正确率</div></div>
          <div class="result-item"><div class="result-num">{{ result.score }}</div><div style="font-size:12px;color:var(--text-secondary);">得分</div></div>
          <div class="result-item"><div class="result-num">{{ formatTimer(result.duration) }}</div><div style="font-size:12px;color:var(--text-secondary);">用时</div></div>
        </div>
        <div style="display:flex;gap:12px;justify-content:center;margin-top:24px;flex-wrap:wrap;">
          <button class="btn btn-outline" @click="exportSmartPdf">📄 导出PDF</button>
          <button class="btn btn-primary" @click="resetConfig">重新组卷</button>
          <button class="btn btn-outline" @click="$router.push('/')">返回首页</button>
        </div>

        <div v-if="knowledgeBreakdown.length > 0" style="margin-top:24px;">
          <h3 style="margin:0 0 12px 0;font-size:17px;color:var(--text);">🎯 知识点分析</h3>
          <div v-for="kb in knowledgeBreakdown" :key="kb.tag" style="background:var(--bg-card);border:1px solid var(--border);border-radius:8px;padding:14px;margin-bottom:12px;">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;gap:8px;">
              <span style="font-weight:600;">{{ kb.tag }}</span>
              <span style="color:var(--text-secondary);font-size:12px;flex-shrink:0;">正确 {{ kb.correct }} · 错误 {{ kb.wrong }} · 正确率 {{ kb.accuracy }}%</span>
            </div>
            <div style="display:flex;justify-content:space-between;align-items:center;gap:8px;">
              <div style="flex:1;height:8px;background:var(--border-light);border-radius:4px;overflow:hidden;">
                <div style="height:100%;border-radius:4px;background:var(--success);" :style="{ width: kb.accuracy + '%' }"></div>
              </div>
              <button class="btn btn-sm" style="background:#059669;color:#fff;border:none;flex-shrink:0;" @click="goKnowledgePractice(kb)">📌 生成该知识点练习</button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import TopNav from '../components/TopNav.vue'
import { formatQuestionContent, formatAnalysis } from '../utils/questionContent'
import { exportPdf, questionBlock } from '../utils/pdfExport'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const phase = ref('config')
const generating = ref(false)
const submitting = ref(false)
const timerSeconds = ref(0)
const examQuestions = ref([])
const examAnswers = ref({})
const examIndex = ref(0)
const result = ref({ total: 0, correct: 0, wrong: 0, accuracy: 0, score: 0, duration: 0 })
const knowledgeTags = ref([])
let timerInterval = null

const subjects = [
  { key: '数据结构', name: '数据结构', icon: '📊' },
  { key: '计算机组成原理', name: '计组原理', icon: '💻' },
  { key: '操作系统', name: '操作系统', icon: '⚙️' },
  { key: '计算机网络', name: '计算机网络', icon: '🌐' }
]
const difficulties = [
  { key: 'easy', label: '简单' },
  { key: 'medium', label: '中等' },
  { key: 'hard', label: '困难' },
  { key: 'mixed', label: '混合' }
]
const allYears = Array.from({ length: 18 }, (_, i) => 2009 + i)

const config = ref({
  subjects: ['数据结构', '计算机组成原理', '操作系统', '计算机网络'],
  count: 20,
  difficulty: 'mixed',
  types: ['单选', '综合应用'],
  yearStart: 2015,
  yearEnd: 2025,
  knowledgeTag: ''
})

const curExamQ = computed(() => examQuestions.value[examIndex.value] || null)
const examAnsweredCount = computed(() => examQuestions.value.filter(q => q.type === '单选' && examAnswers.value[q.id]).length)
const examProgressPercent = computed(() => singleCount.value ? Math.round(examAnsweredCount.value / singleCount.value * 100) : 0)
const singleCount = computed(() => examQuestions.value.filter(q => q.type === '单选').length)
const knowledgeBreakdown = computed(() => {
  const map = {}
  for (const q of examQuestions.value) {
    if (q.type !== '单选') continue
    const tag = q.knowledgeTag || '未标注知识点'
    if (!map[tag]) map[tag] = { tag, subject: q.subject, total: 0, correct: 0, wrong: 0 }
    map[tag].total++
    if (examAnswers.value[q.id] === q.answer) map[tag].correct++
    else map[tag].wrong++
  }
  return Object.keys(map).map(k => ({
    tag: map[k].tag,
    subject: map[k].subject,
    correct: map[k].correct,
    wrong: map[k].wrong,
    accuracy: map[k].total > 0 ? Math.round(map[k].correct / map[k].total * 100) : 0
  }))
})

function toggleSubject(key) {
  const idx = config.value.subjects.indexOf(key)
  if (idx >= 0) config.value.subjects.splice(idx, 1)
  else config.value.subjects.push(key)
  loadKnowledgeTags()
}

async function loadKnowledgeTags() {
  if (config.value.subjects.length > 0) {
    try {
      const r = await api.getKnowledgeTags(config.value.subjects[0])
      if (r.data.code === 200) {
        knowledgeTags.value = r.data.data || []
      }
    } catch (e) {
      console.error('获取知识点失败', e)
    }
  } else {
    knowledgeTags.value = []
  }
}

function onKnowledgeTagChange() {
}

function parseOptions(o) {
  if (!o) return []
  if (typeof o === 'string') { try { return JSON.parse(o) } catch (e) { return [] } }
  return o
}

function formatTimer(s) {
  const m = Math.floor(s / 60)
  const se = s % 60
  return `${String(m).padStart(2, '0')}:${String(se).padStart(2, '0')}`
}

async function generateExam() {
  generating.value = true
  try {
    const params = {
      subject: config.value.subjects.length > 0 ? config.value.subjects[0] : '',
      knowledgeTag: config.value.knowledgeTag || undefined,
      type: config.value.types.length > 0 ? config.value.types[0] : '',
      yearStart: config.value.yearStart,
      yearEnd: config.value.yearEnd,
      limit: config.value.count
    }
    const r = await api.filterQuestions(params)
    if (r.data.code === 200) {
      const data = r.data.data
      const rawList = Array.isArray(data) ? data : []
      const uniqueList = removeDuplicates(rawList)
      examQuestions.value = uniqueList.map(q => ({ ...q }))
      if (examQuestions.value.length === 0) {
        alert('无法找到符合条件的题目，请调整筛选条件')
        return
      }
      examAnswers.value = {}
      examIndex.value = 0
      timerSeconds.value = 0
      phase.value = 'exam'
      startTimer()
    }
  } catch (e) { alert('获取题目失败，请重试') }
  generating.value = false
}

function removeDuplicates(questions) {
  const seen = new Set()
  return questions.filter(q => {
    if (q.id && !seen.has(q.id)) {
      seen.add(q.id)
      return true
    }
    return false
  })
}

function startTimer() {
  if (timerInterval) clearInterval(timerInterval)
  timerInterval = setInterval(() => { timerSeconds.value++ }, 1000)
}

function stopTimer() {
  if (timerInterval) { clearInterval(timerInterval); timerInterval = null }
}

async function submitExam() {
  submitting.value = true
  try {
    // 综合应用题仅展示答案，只提交单选题
    const answerList = examQuestions.value
      .filter(q => q.type === '单选')
      .map(q => ({
        questionId: q.id,
        userAnswer: examAnswers.value[q.id] || ''
      }))
    const r = await api.submitAnswers({
      mode: 'exam',
      answers: answerList,
      durationSeconds: timerSeconds.value
    })
    if (r.data.code === 200) {
      const d = r.data.data
      const total = singleCount.value || 0
      const correct = d.correctCount || 0
      result.value = {
        total,
        correct,
        wrong: Math.max(0, total - correct),
        accuracy: total > 0 ? (correct / total * 100).toFixed(1) : 0,
        score: d.score || 0,
        duration: timerSeconds.value
      }
      phase.value = 'result'
      stopTimer()
    }
  } catch (e) { alert('提交失败') }
  submitting.value = false
}

function resetConfig() {
  stopTimer()
  examQuestions.value = []
  examAnswers.value = {}
  examIndex.value = 0
  timerSeconds.value = 0
  result.value = { total: 0, correct: 0, wrong: 0, accuracy: 0, score: 0, duration: 0 }
  phase.value = 'config'
}

function goKnowledgePractice(kb) {
  if (!kb || !kb.tag) return
  router.push({ path: '/practice', query: { subject: kb.subject || '', knowledgeTag: kb.tag } })
}

function exportSmartPdf() {
  const items = examQuestions.value.map(q => ({
    year: q.year || q.examYear,
    questionNumber: q.questionNumber,
    type: q.type,
    knowledgeTag: q.knowledgeTag,
    content: formatQuestionContent(q.content),
    options: parseOptions(q.options, q.questionNumber, q.year || q.examYear),
    userAnswer: q.type === '综合应用' ? '' : (examAnswers.value[q.id] || ''),
    answer: q.answer,
    essay: q.type === '综合应用',
    correct: q.type === '综合应用' ? undefined : (examAnswers.value[q.id] === q.answer),
    analysis: formatAnalysis(q.analysis)
  }))
  const sub = `科目：${config.value.subjects.join(' / ')} · 题量 ${result.value.total} · 得分 ${result.value.score} · 正确率 ${result.value.accuracy}% · 用时 ${formatTimer(result.value.duration)}`
  if (!exportPdf({ title: '智能组卷考试结果', subtitle: sub, blocks: items.map(questionBlock) })) {
    alert('请允许浏览器打开新窗口后重试（用于生成 PDF）')
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

function exitSmartExam() {
  if (!confirm('确定要退出本次考试吗？当前进度将不会保存。')) return
  stopTimer()
  resetConfig()
}

function exportCurrentPdf() {
  const qs = examQuestions.value
  if (!qs.length) { alert('当前没有可导出的题目'); return }
  const items = qs.map(q => ({
    year: q.year || q.examYear,
    questionNumber: q.questionNumber,
    type: q.type,
    knowledgeTag: q.knowledgeTag,
    content: formatQuestionContent(q.content),
    options: parseOptions(q.options, q.questionNumber, q.year || q.examYear),
    userAnswer: q.type === '综合应用' ? '' : (examAnswers.value[q.id] || ''),
    answer: q.answer,
    essay: q.type === '综合应用',
    correct: q.type === '综合应用' ? undefined : (examAnswers.value[q.id] === q.answer),
    analysis: formatAnalysis(q.analysis)
  }))
  const sub = `科目：${config.value.subjects.join(' / ')} · 题量 ${qs.length} · 用时 ${formatTimer(timerSeconds.value)}`
  if (!exportPdf({ title: '智能组卷试卷', subtitle: sub, blocks: items.map(questionBlock) })) {
    alert('请允许浏览器打开新窗口后重试（用于生成 PDF）')
  }
}

function toggleTheme() {
  document.body.classList.toggle('dark-mode')
}

function logout() {
  try { api.logout() } catch (e) {}
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  user.value = JSON.parse(localStorage.getItem('user') || 'null')
  loadKnowledgeTags()
})

onBeforeUnmount(() => {
  stopTimer()
})
</script>

<style scoped>
.se-root { min-height: 100vh; background: var(--bg); }
.se-body { max-width: 1000px; margin: 0 auto; padding: 32px 20px; }
.analysis-box { background: var(--bg-card); border: 1px solid var(--border); border-left: 3px solid #1565c0; border-radius: 8px; padding: 8px 12px; font-size: 13px; line-height: 1.55; }
.analysis-box .an-item { display: inline; margin-right: 16px; white-space: pre-wrap; }
.dark-mode .analysis-box { background: #0f172a; border-color: #334155; }
.se-header { text-align: center; margin-bottom: 32px; }
.se-title { font-size: 28px; font-weight: 700; color: var(--text); margin-bottom: 8px; }
.se-desc { font-size: 15px; color: var(--text-secondary); }
.se-config-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.se-config-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 20px;
  transition: var(--transition);
}
.se-config-card:hover { box-shadow: var(--shadow-md); }
.se-config-title { font-size: 15px; font-weight: 600; margin-bottom: 14px; color: var(--text); }
.se-summary-card { grid-column: 1 / -1; }
.se-subject-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.se-subject-btn {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border: 1px solid var(--border); border-radius: 8px;
  background: var(--bg); cursor: pointer; font-size: 13px;
  transition: var(--transition); color: var(--text);
}
.se-subject-btn:hover { border-color: var(--primary); }
.se-subject-btn.active { background: var(--primary-glow); border-color: var(--primary); color: var(--primary); font-weight: 600; }
.se-subj-icon { font-size: 18px; }
.se-slider-row { display: flex; align-items: center; gap: 12px; }
.se-slider { flex: 1; accent-color: var(--primary); height: 6px; }
.se-slider-val { font-size: 20px; font-weight: 700; color: var(--primary); min-width: 50px; text-align: center; }
.se-difficulty-row { display: flex; gap: 8px; flex-wrap: wrap; }
.se-diff-btn { padding: 8px 16px; border: 1px solid var(--border); border-radius: 8px; background: var(--bg); cursor: pointer; font-size: 13px; transition: var(--transition); color: var(--text); }
.se-diff-btn:hover { border-color: var(--primary); }
.se-diff-btn.active { background: var(--primary-glow); border-color: var(--primary); color: var(--primary); font-weight: 600; }
.se-type-row { display: flex; gap: 16px; }
.se-check-label { display: flex; align-items: center; gap: 6px; cursor: pointer; font-size: 14px; color: var(--text); }
.se-check-label input { accent-color: var(--primary); }
.se-year-row { display: flex; align-items: center; gap: 8px; }
.se-knowledge-row { display: flex; align-items: center; gap: 8px; }
.se-select { padding: 8px 12px; border: 1px solid var(--border); border-radius: 8px; font-size: 13px; background: var(--bg-card); color: var(--text); min-width: 200px; }
.se-summary-items { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 16px; }
.se-summary-item { display: flex; justify-content: space-between; padding: 6px 0; font-size: 14px; color: var(--text-secondary); border-bottom: 1px solid var(--border-light); }
.se-summary-item strong { color: var(--text); }
.se-generate-btn { width: 100%; padding: 14px; background: linear-gradient(135deg, var(--primary), var(--primary-light)); color: #fff; border: none; border-radius: 10px; font-size: 16px; font-weight: 600; cursor: pointer; transition: var(--transition); }
.se-generate-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 25px var(--primary-glow); }
.se-generate-btn:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

.exam-topbar { display: flex; align-items: center; gap: 16px; padding: 14px 20px; background: var(--bg-card); border: 1px solid var(--border); border-radius: 12px; margin-bottom: 20px; flex-wrap: wrap; }
.exam-topbar .question-item-wrap { max-width: 820px; margin: 0 auto; }
.se-body .question-item { max-width: 820px; margin-left: auto; margin-right: auto; padding: 18px 20px; }
.exam-timer { font-size: 24px; font-weight: 700; font-variant-numeric: tabular-nums; color: var(--primary); }
.exam-progress { font-size: 14px; color: var(--text-secondary); }
.se-nav-buttons { display: flex; justify-content: space-between; align-items: center; margin-top: 16px; }

@media (max-width: 768px) {
  .se-config-grid { grid-template-columns: 1fr; }
  .se-body { padding: 20px 12px; }
  .se-title { font-size: 22px; }
}
</style>
