<template>
  <div class="page-root" :class="theme">
    <TopNav :username="user?.nickname||'用户'" @toggle-theme="toggleTheme" @logout="logout" />
    <div class="page-body">
      <main class="page-content">
      <div class="page-header">
        <h2>学习统计</h2>
        <button class="btn btn-primary btn-sm" @click="$router.push('/report')">📄 导出月学习报告</button>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalQuestions || 0 }}</div>
          <div class="stat-label">总题数</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:var(--success);">{{ stats.totalCorrect || 0 }}</div>
          <div class="stat-label">正确数</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.totalAccuracy || 0 }}%</div>
          <div class="stat-label">正确率</div>
        </div>
        <div class="stat-card">
          <div class="stat-value" style="color:var(--danger);">{{ stats.wrongCount || 0 }}</div>
          <div class="stat-label">错题数</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ stats.examCount || 0 }}</div>
          <div class="stat-label">考试次数</div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">各科目正确率</div>
        <div v-for="subject in subjects" :key="subject.key" style="margin-bottom:12px;">
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
            <span style="font-size:14px; font-weight:500;">{{ subject.label }}</span>
            <span style="font-size:13px; color:var(--text-secondary);">{{ subject.accuracy }}%</span>
          </div>
          <div class="accuracy-bar">
            <div class="accuracy-fill" :class="subject.level" :style="{ width: subject.accuracy + '%' }"></div>
          </div>
        </div>
      </div>

      <!-- 掌握度雷达 + 覆盖率 -->
      <div class="card">
        <div class="card-title">掌握度雷达与题目覆盖率</div>
        <div class="mastery-grid">
          <div class="radar-box">
            <svg :viewBox="`0 0 ${radar.size} ${radar.size}`" class="radar-svg">
              <g v-for="r in 4" :key="'ring' + r">
                <polygon :points="radar.ringPoints(r * 25)" fill="none" stroke="var(--border)" stroke-width="1" />
              </g>
              <line
                v-for="(s, i) in masterySubs" :key="'axis' + i"
                :x1="radar.center" :y1="radar.center"
                :x2="radar.axisPoint(s, 100).x" :y2="radar.axisPoint(s, 100).y"
                stroke="var(--border)" stroke-width="1"
              />
              <polygon :points="radar.dataPolygon" fill="rgba(5, 150, 105, 0.25)" stroke="var(--success)" stroke-width="2" stroke-linejoin="round" />
              <g v-for="(s, i) in masterySubs" :key="'lab' + i">
                <circle :cx="radar.axisPoint(s, Math.max(4, s.mastery)).x" :cy="radar.axisPoint(s, Math.max(4, s.mastery)).y" r="3.5" fill="var(--success)" />
                <text :x="radar.axisPoint(s, 130).x" :y="radar.axisPoint(s, 130).y" text-anchor="middle" class="radar-name">{{ s.name }}</text>
                <text :x="radar.axisPoint(s, 108).x" :y="radar.axisPoint(s, 108).y" text-anchor="middle" class="radar-val">掌握 {{ Math.round(s.mastery) }}%</text>
              </g>
            </svg>
            <div class="radar-tip">数值 = 已掌握题目数 / 该科题库总数</div>
          </div>
          <div class="coverage-box">
            <div class="coverage-head">
              <span>总覆盖率</span>
              <span class="coverage-num">{{ masteryData.coverage || 0 }}%</span>
            </div>
            <div class="accuracy-bar" style="margin-bottom:18px;">
              <div class="accuracy-fill high" :style="{ width: (masteryData.coverage || 0) + '%' }"></div>
            </div>
            <div v-for="ms in masterySubs" :key="ms.name" style="margin-bottom:12px;">
              <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
                <span style="font-size:14px; font-weight:500;">{{ ms.name }}</span>
                <span style="font-size:13px; color:var(--text-secondary);">{{ ms.answered }}/{{ ms.total }} 题 · {{ ms.mastery }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: ms.answeredCoverage + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 考点 × 年份 出题热力图 -->
      <div class="card">
        <div class="card-title">考点 × 年份 出题热力图（真题分布）</div>
        <div class="heat-tip">行 = 章节考点，列 = 年份，颜色深浅 = 该考点在该年份出题数量</div>
        <div v-for="subj in heatmap.subjects" :key="subj.name" class="heat-subject">
          <div class="heat-subject-name">{{ subj.name }}</div>
          <div class="heat-wrap">
            <table class="heat-table">
              <thead>
                <tr>
                  <th class="heat-ch-label">章节</th>
                  <th v-for="y in heatmap.years" :key="y" class="heat-year">{{ y }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in subj.rows" :key="row.label">
                  <td class="heat-ch-label">{{ row.label }}</td>
                  <td v-for="(c, i) in row.counts" :key="i" class="heat-cell-wrap">
                    <span class="heat-cell" :class="heatLevel(c)" :title="heatmap.years[i] + '年 ' + subj.name + ' ' + row.label + '：' + c + '题'">
                      {{ c > 0 ? c : '' }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
        <div class="heat-legend">
          <span>少</span>
          <span class="heat-cell lv0"></span>
          <span class="heat-cell lv1"></span>
          <span class="heat-cell lv2"></span>
          <span class="heat-cell lv3"></span>
          <span class="heat-cell lv4"></span>
          <span>多</span>
        </div>
      </div>

      <div class="card">
        <div class="card-title">练习历史</div>
        <table v-if="history.length > 0" style="width:100%; border-collapse:collapse; font-size:14px;">
          <thead>
            <tr style="border-bottom:2px solid var(--border); text-align:left;">
              <th style="padding:10px 12px;">日期</th>
              <th style="padding:10px 12px;">模式</th>
              <th style="padding:10px 12px;">科目</th>
              <th style="padding:10px 12px;">正确/总数</th>
              <th style="padding:10px 12px;">正确率</th>
              <th style="padding:10px 12px;">得分</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in history" :key="record.id" style="border-bottom:1px solid var(--border);">
              <td style="padding:10px 12px;">{{ formatDate(record.createdAt) }}</td>
              <td style="padding:10px 12px;">{{ modeLabel(record.mode) }}</td>
              <td style="padding:10px 12px;">{{ record.subject || '-' }}</td>
              <td style="padding:10px 12px;">{{ record.correctCount }}/{{ record.totalQuestions }}</td>
              <td style="padding:10px 12px;">
                <span :style="{ color: record.accuracy >= 80 ? 'var(--success)' : record.accuracy >= 60 ? 'var(--warning)' : 'var(--danger)' }">
                  {{ record.accuracy }}%
                </span>
              </td>
              <td style="padding:10px 12px; font-weight:600;">{{ record.score }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else style="text-align:center; color:var(--text-secondary); padding:20px 0;">
          暂无练习记录
        </div>
      </div>

      <!-- AI 学情诊断卡片 -->
      <div class="card diagnosis-card">
        <div class="diagnosis-head">
          <div class="card-title" style="margin:0;">🤖 AI 学情诊断</div>
          <button v-if="!diagnosisLoading" class="btn btn-primary btn-sm diagnosis-btn" @click="runDiagnosis" :disabled="diagnosisLoading">
            {{ diagnosis ? '重新诊断' : '开始诊断' }}
          </button>
        </div>

        <div v-if="diagnosisLoading" class="diagnosis-loading">
          <div class="diagnosis-spinner"></div>
          <span>AI 正在分析你的学情数据…</span>
        </div>

        <div v-else-if="diagnosis">
          <!-- 结构化结果 -->
          <template v-if="diagnosis.structured">
            <div class="diagnosis-summary">{{ diagnosis.summary }}</div>

            <div v-if="diagnosis.weakPoints && diagnosis.weakPoints.length" class="diagnosis-section">
              <div class="diagnosis-section-title">⚠️ 薄弱点</div>
              <div v-for="(wp, i) in diagnosis.weakPoints" :key="'wp'+i" class="diagnosis-item">
                <div class="diagnosis-item-head">
                  <span class="diagnosis-badge">{{ wp.subject }}</span>
                  <span v-if="wp.tag" class="diagnosis-tag">{{ wp.tag }}</span>
                  <span v-if="wp.accuracy !== undefined" class="diagnosis-acc" :class="{ low: wp.accuracy < 50 }">{{ wp.accuracy }}%</span>
                </div>
                <div v-if="wp.reason" class="diagnosis-reason">{{ wp.reason }}</div>
              </div>
            </div>

            <div v-if="diagnosis.studyPlan && diagnosis.studyPlan.length" class="diagnosis-section">
              <div class="diagnosis-section-title">📅 学习计划</div>
              <div v-for="(sp, i) in diagnosis.studyPlan" :key="'sp'+i" class="diagnosis-plan-item">
                <span class="diagnosis-day">Day {{ sp.day }}</span>
                <span>{{ sp.task }}</span>
              </div>
            </div>

            <div v-if="diagnosis.recommendFilter && diagnosis.recommendFilter.subject" class="diagnosis-recommend">
              <div class="diagnosis-recommend-info">
                <div class="diagnosis-recommend-title">🎯 推荐练习</div>
                <div class="diagnosis-recommend-detail">
                  {{ diagnosis.recommendFilter.subject }}
                  <span v-if="diagnosis.recommendFilter.tag"> / {{ diagnosis.recommendFilter.tag }}</span>
                </div>
                <div v-if="diagnosis.recommendFilter.reason" class="diagnosis-reason">{{ diagnosis.recommendFilter.reason }}</div>
              </div>
              <button class="btn btn-primary btn-sm" @click="goPractice(diagnosis.recommendFilter)">去练习 →</button>
            </div>
          </template>

          <!-- 降级纯文本 -->
          <div v-else class="diagnosis-raw" v-html="renderMarkdown(diagnosis.rawText)"></div>
        </div>

        <div v-else class="diagnosis-empty">
          点击「开始诊断」，AI 将分析你的答题数据，找出薄弱点并推荐练习方向。
        </div>
      </div>
    </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import TopNav from '../components/TopNav.vue'
import { renderMarkdown } from '../utils/markdown'

const router = useRouter()

const user = ref(null)
const stats = ref({})
const history = ref([])
const theme = ref(localStorage.getItem('theme') || 'light')

const heatmap = ref({ years: [], subjects: [], maxCount: 1 })
const masteryData = ref({ coverage: 0, subjects: [] })

// AI 学情诊断
const diagnosis = ref(null)
const diagnosisLoading = ref(false)

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('theme', theme.value)
}

const subjects = computed(() => {
  const list = [
    { key: 'ds', label: '数据结构', accuracy: stats.value.dsAccuracy || 0 },
    { key: 'co', label: '计算机组成原理', accuracy: stats.value.coAccuracy || 0 },
    { key: 'os', label: '操作系统', accuracy: stats.value.osAccuracy || 0 },
    { key: 'cn', label: '计算机网络', accuracy: stats.value.cnAccuracy || 0 }
  ]
  return list.map(s => ({
    ...s,
    level: s.accuracy >= 80 ? 'high' : s.accuracy >= 60 ? 'medium' : 'low'
  }))
})

const modeLabel = (mode) => {
  const map = {
    year: '年份套题',
    subject: '单科专项',
    random: '随机刷题',
    wrong: '错题重刷'
  }
  return map[mode] || mode
}

// ===== 掌握度雷达（手写 SVG） =====
const masterySubs = computed(() => {
  return (masteryData.value.subjects || []).map(s => ({
    ...s,
    answeredCoverage: s.total > 0 ? Math.round(s.answered / s.total * 100) : 0
  }))
})

const radar = computed(() => {
  const size = 300
  const center = 150
  const radius = 105
  const subs = masterySubs.value
  const pos = (val, angleDeg) => {
    const ang = (angleDeg - 90) * Math.PI / 180
    const r = radius * Math.min(100, Math.max(0, val)) / 100
    return { x: center + r * Math.cos(ang), y: center + r * Math.sin(ang) }
  }
  const ringPoints = (val) => subs.map((s, i) => {
    const p = pos(val, i * 90)
    return `${p.x},${p.y}`
  }).join(' ')
  const dataPolygon = subs.map((s, i) => {
    const p = pos(s.mastery || 0, i * 90)
    return `${p.x},${p.y}`
  }).join(' ')
  return {
    size, center, radius,
    ringPoints,
    dataPolygon,
    axisPoint: (s, val) => {
      const i = subs.findIndex(x => x.name === s.name)
      return pos(val, (i < 0 ? 0 : i) * 90)
    }
  }
})

// ===== 热力图颜色分级 =====
const heatLevel = (count) => {
  if (count <= 0) return 'lv0'
  const max = heatmap.value.maxCount || 1
  const r = count / max
  if (r <= 0.35) return 'lv1'
  if (r <= 0.65) return 'lv2'
  if (r <= 0.85) return 'lv3'
  return 'lv4'
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const runDiagnosis = async () => {
  diagnosisLoading.value = true
  diagnosis.value = null
  try {
    const r = await api.getDiagnosis()
    if (r.data.code === 200) {
      diagnosis.value = r.data.data
    } else {
      diagnosis.value = { structured: false, rawText: '诊断失败：' + (r.data.message || '未知错误') }
    }
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '网络异常'
    diagnosis.value = { structured: false, rawText: '诊断失败：' + msg }
  } finally {
    diagnosisLoading.value = false
  }
}

const goPractice = (filter) => {
  const query = {}
  if (filter.subject) query.subject = filter.subject
  if (filter.tag) query.knowledgeTag = filter.tag
  router.push({ path: '/practice', query })
}

const logout = async () => {
  try {
    await api.logout()
  } catch (e) {
    // ignore
  }
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(async () => {
  const raw = localStorage.getItem('user')
  user.value = raw ? JSON.parse(raw) : null

  try {
    const statsR = await api.getStats()
    if (statsR.data.code === 200) {
      stats.value = statsR.data.data || {}
    }
  } catch (e) {
    // ignore
  }

  try {
    const histR = await api.getHistory()
    if (histR.data.code === 200) {
      history.value = histR.data.data || []
    }
  } catch (e) {
    // ignore
  }

  try {
    const heatR = await api.getHeatmap()
    if (heatR.data.code === 200) {
      heatmap.value = heatR.data.data || { years: [], subjects: [], maxCount: 1 }
    }
  } catch (e) {
    // ignore
  }

  try {
    const masR = await api.getMastery()
    if (masR.data.code === 200) {
      masteryData.value = masR.data.data || { coverage: 0, subjects: [] }
    }
  } catch (e) {
    // ignore
  }
})
</script>

<style scoped>
.page-root { min-height:100vh; }
.page-body { display:flex; }
.page-content { flex:1; padding:20px; }
.page-header { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; }

/* 掌握度雷达 + 覆盖率 */
.mastery-grid { display:flex; gap:24px; flex-wrap:wrap; }
.radar-box { flex:1; min-width:260px; max-width:340px; }
.radar-svg { width:100%; height:auto; display:block; }
.radar-name { font-size:13px; font-weight:600; fill:var(--text); }
.radar-val { font-size:11px; fill:var(--success); font-weight:600; }
.radar-tip { font-size:12px; color:var(--text-muted); text-align:center; margin-top:4px; }
.coverage-box { flex:1; min-width:280px; }
.coverage-head {
  display:flex; justify-content:space-between; align-items:center;
  font-size:14px; font-weight:600; margin-bottom:4px;
}
.coverage-num { font-size:20px; font-weight:700; color:var(--primary); }

/* 热力图 */
.heat-tip { font-size:13px; color:var(--text-secondary); margin-bottom:14px; }
.heat-subject { margin-bottom:20px; }
.heat-subject-name {
  font-size:14px; font-weight:700; margin-bottom:8px; padding-left:10px;
  border-left:3px solid var(--primary);
}
.heat-wrap { overflow-x:auto; }
.heat-table { border-collapse:collapse; width:100%; font-size:12px; }
.heat-table th, .heat-table td { padding:2px; text-align:center; }
.heat-year { font-size:11px; color:var(--text-muted); font-weight:400; min-width:26px; }
.heat-ch-label {
  font-size:12px; text-align:left; color:var(--text-secondary);
  white-space:nowrap; padding-right:8px; font-weight:500;
}
.heat-cell-wrap { padding:2px; }
.heat-cell {
  display:inline-flex; align-items:center; justify-content:center;
  width:100%; min-width:22px; height:22px; border-radius:4px;
  font-size:11px; font-weight:600;
}
.heat-cell.lv0 { background:var(--border-light); color:transparent; }
.heat-cell.lv1 { background:#d6f0e2; color:#1a7a4e; }
.heat-cell.lv2 { background:#9fdcc0; color:#0f613f; }
.heat-cell.lv3 { background:#4bb98c; color:#fff; }
.heat-cell.lv4 { background:#0d7a4c; color:#fff; }
.heat-legend {
  display:flex; align-items:center; gap:6px; justify-content:flex-end;
  font-size:12px; color:var(--text-secondary); margin-top:4px;
}
.heat-legend .heat-cell { width:18px; min-width:18px; height:18px; }

/* AI 学情诊断卡片 */
.diagnosis-card { border: 1px solid rgba(124, 58, 237, 0.2); }
.diagnosis-head {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px;
}
.diagnosis-btn { background: #7c3aed; border: none; }
.diagnosis-btn:hover { background: #6d28d9; }
.diagnosis-loading {
  display: flex; align-items: center; gap: 10px;
  padding: 30px 0; color: var(--text-secondary); font-size: 14px;
}
.diagnosis-spinner {
  width: 18px; height: 18px; border: 2px solid var(--border);
  border-top-color: #7c3aed; border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.diagnosis-empty {
  text-align: center; color: var(--text-secondary); font-size: 14px;
  padding: 24px 0; line-height: 1.7;
}
.diagnosis-summary {
  font-size: 15px; font-weight: 600; color: #7c3aed;
  padding: 10px 14px; background: rgba(124, 58, 237, 0.06);
  border-radius: 8px; margin-bottom: 16px; line-height: 1.6;
}
.diagnosis-section { margin-bottom: 16px; }
.diagnosis-section-title {
  font-size: 14px; font-weight: 600; margin-bottom: 10px; color: var(--text);
}
.diagnosis-item {
  padding: 10px 12px; background: var(--bg, #f8f9fa); border-radius: 8px;
  margin-bottom: 8px; border: 1px solid var(--border, #e5e7eb);
}
.diagnosis-item-head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.diagnosis-badge {
  font-size: 12px; font-weight: 600; padding: 2px 8px;
  background: rgba(5, 150, 105, 0.12); color: #059669; border-radius: 4px;
}
.diagnosis-tag {
  font-size: 12px; color: var(--text-secondary);
  background: var(--border-light, #f3f4f6); padding: 2px 8px; border-radius: 4px;
}
.diagnosis-acc { font-size: 13px; font-weight: 700; color: var(--success); }
.diagnosis-acc.low { color: var(--danger); }
.diagnosis-reason { font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
.diagnosis-plan-item {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 6px 0; font-size: 14px; line-height: 1.6;
}
.diagnosis-day {
  font-size: 12px; font-weight: 700; color: #7c3aed;
  background: rgba(124, 58, 237, 0.1); padding: 2px 8px; border-radius: 4px;
  white-space: nowrap; flex-shrink: 0;
}
.diagnosis-recommend {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 16px; background: linear-gradient(135deg, rgba(5, 150, 105, 0.08), rgba(5, 150, 105, 0.04));
  border: 1px solid rgba(5, 150, 105, 0.25); border-radius: 10px; margin-top: 8px;
}
.diagnosis-recommend-title { font-size: 14px; font-weight: 700; color: var(--success); }
.diagnosis-recommend-detail { font-size: 15px; font-weight: 600; margin-top: 4px; }
.diagnosis-recommend .diagnosis-reason { margin-top: 4px; }
.diagnosis-raw { line-height: 1.7; font-size: 14px; }
.diagnosis-raw :deep(.ai-p) { margin: 4px 0; }
.diagnosis-raw :deep(.ai-ul), .diagnosis-raw :deep(.ai-ol) { margin: 4px 0; padding-left: 18px; }
</style>
