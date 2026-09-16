<template>
  <div class="page-root" :class="theme">
    <div class="noprint">
      <TopNav :username="user?.nickname || '用户'" @toggle-theme="toggleTheme" @logout="logout" />
    </div>
    <div class="page-body">
      <main class="page-content">
        <div class="page-header report-toolbar noprint">
          <h2>月学习报告</h2>
          <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
            <input type="month" class="form-input" v-model="month" @change="load" />
            <button class="btn btn-primary btn-sm" @click="doPrint">🖨️ 导出 PDF</button>
            <button class="btn btn-outline btn-sm" @click="$router.push('/stats')">← 返回统计</button>
          </div>
        </div>

        <div class="report-sheet" ref="sheet">
          <div class="report-title-block">
            <h1>408 考研真题训练 · 月度学习报告</h1>
            <div class="report-meta">
              <span>统计月份：{{ report.month || month }}</span>
              <span>生成时间：{{ generatedAt }}</span>
            </div>
          </div>

          <div v-if="!loaded" style="text-align:center; padding:60px 0; color:var(--text-secondary);">
            报告加载中…
          </div>

          <template v-else>
            <!-- 概述 -->
            <div class="report-summary">
              <div class="rs-item">
                <div class="rs-num">{{ report.answered || 0 }}</div>
                <div class="rs-label">去重作答数</div>
              </div>
              <div class="rs-item">
                <div class="rs-num">{{ report.submissions || 0 }}</div>
                <div class="rs-label">提交次数</div>
              </div>
              <div class="rs-item">
                <div class="rs-num" style="color:var(--success);">{{ report.correct || 0 }}</div>
                <div class="rs-label">做对题数</div>
              </div>
              <div class="rs-item">
                <div class="rs-num" style="color:#3b82f6;">{{ report.accuracy || 0 }}%</div>
                <div class="rs-label">正确率</div>
              </div>
              <div class="rs-item">
                <div class="rs-num">{{ report.studyDays || 0 }}</div>
                <div class="rs-label">学习天数</div>
              </div>
              <div class="rs-item">
                <div class="rs-num" style="color:var(--danger);">{{ report.wrongAdded || 0 }}</div>
                <div class="rs-label">新增错题</div>
              </div>
            </div>

            <!-- 科目表现 -->
            <h3 class="report-section-title">📊 各科目表现</h3>
            <table class="report-table">
              <thead>
                <tr>
                  <th>科目</th>
                  <th>作答数</th>
                  <th>做对数</th>
                  <th>正确率</th>
                  <th>完成度</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in report.subjects" :key="s.name">
                  <td>{{ s.name }}</td>
                  <td>{{ s.answered }}</td>
                  <td>{{ s.correct }}</td>
                  <td>
                    <span :style="{ color: s.accuracy >= 80 ? 'var(--success)' : s.accuracy >= 60 ? 'var(--warning)' : 'var(--danger)' }">
                      {{ s.accuracy }}%
                    </span>
                  </td>
                  <td style="min-width:140px;">
                    <div class="progress-bar">
                      <div class="progress-fill" :style="{ width: Math.min(100, s.accuracy) + '%' }"></div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- 每日趋势 -->
            <h3 class="report-section-title">📈 每日练习趋势</h3>
            <div v-if="trend.length > 0" class="trend-wrap">
              <div class="trend-bars">
                <div
                  v-for="t in trend" :key="t.date"
                  class="trend-bar-col"
                  :title="t.date + '：提交 ' + t.submissions + ' 次，做对 ' + t.correct + ' 题'"
                >
                  <div class="trend-bar" :style="{ height: trendHeight(t.submissions) + '%' }">
                    <span v-if="t.submissions > 0" class="trend-bar-num">{{ t.submissions }}</span>
                  </div>
                  <div class="trend-bar-day">{{ dayOf(t.date) }}</div>
                </div>
              </div>
              <div class="trend-legend">每日提交次数（mm/dd）</div>
            </div>
            <div v-else style="color:var(--text-secondary); font-size:14px; padding:12px 0;">
              本月暂无练习记录
            </div>

            <!-- 错题原因 -->
            <h3 class="report-section-title">📌 错题情况</h3>
            <div class="report-wrong-grid">
              <div class="rw-cell">
                <div class="rw-label">本月新增错题</div>
                <div class="rw-num" style="color:var(--danger);">{{ report.wrongAdded || 0 }}</div>
              </div>
              <div class="rw-cell">
                <div class="rw-label">目前错题总数</div>
                <div class="rw-num">{{ report.wrongTotal || 0 }}</div>
              </div>
              <div class="rw-cell">
                <div class="rw-label">已标注原因</div>
                <div class="rw-num" style="color:var(--success);">{{ report.wrongReasonTagged || 0 }}</div>
              </div>
            </div>
            <div v-if="(report.wrongAdded || 0) > 0 && report.wrongReasonTagged > 0" class="report-reason">
              <div v-for="r in reasonOptions" :key="r">
                <div style="display:flex; justify-content:space-between; font-size:13px; margin-bottom:4px;">
                  <span>{{ r }}</span>
                  <span>{{ report.wrongReason?.[r] || 0 }} 题</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: wrongReasonPercent(r) + '%', background: reasonColor(r) }"></div>
                </div>
              </div>
            </div>

            <div class="report-footer-note">
              本报告由 408 真题训练系统基于答题记录自动生成，仅供复习参考。
            </div>
          </template>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import TopNav from '../components/TopNav.vue'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const theme = ref(localStorage.getItem('theme') || 'light')

const month = ref(new Date().toISOString().slice(0, 7))
const report = ref({})
const loaded = ref(false)
const generatedAt = ref('')
const reasonOptions = ['概念不清', '粗心', '计算错', '审题不清', '其他']

const trend = computed(() => report.value.trend || [])
const trendMax = computed(() => {
  let m = 0
  for (const t of trend.value) m = Math.max(m, t.submissions)
  return m || 1
})

const trendHeight = (n) => Math.max(4, Math.round(n / trendMax.value * 100))
const dayOf = (date) => {
  const parts = (date || '').split('-')
  return parts.length >= 3 ? `${parts[1]}/${parts[2]}` : date
}

const wrongReasonPercent = (r) => {
  const added = report.value.wrongAdded || 0
  if (!added) return 0
  return Math.round((report.value.wrongReason?.[r] || 0) / added * 100)
}

const reasonColor = (r) => {
  const colors = { '概念不清': '#3b82f6', '粗心': '#f59e0b', '计算错': '#ef4444', '审题不清': '#8b5cf6', '其他': '#64748b' }
  return colors[r] || '#059669'
}

const load = async () => {
  loaded.value = false
  try {
    const r = await api.getMonthlyReport(month.value)
    if (r.data.code === 200) {
      report.value = r.data.data || {}
      generatedAt.value = formatNow()
    }
  } catch (e) {
    report.value = {}
  } finally {
    loaded.value = true
  }
}

const formatNow = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

const doPrint = () => window.print()

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('theme', theme.value)
}

function logout() {
  try { api.logout() } catch (e) {}
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(load)
</script>

<style scoped>
.page-root { min-height:100vh; }
.page-body { display:flex; }
.page-content { flex:1; padding:20px; }
.report-toolbar { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px; }

.report-sheet {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 32px 28px;
  box-shadow: var(--shadow-sm);
}

.report-title-block {
  text-align: center;
  border-bottom: 2px solid var(--border);
  padding-bottom: 18px;
  margin-bottom: 24px;
}
.report-title-block h1 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 8px;
}
.report-meta {
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  gap: 24px;
  justify-content: center;
}

.report-summary {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
.rs-item {
  text-align: center;
  padding: 14px 8px;
  background: var(--border-light);
  border-radius: 10px;
}
.rs-num { font-size: 24px; font-weight: 700; color: var(--text); }
.rs-label { font-size: 12px; color: var(--text-secondary); margin-top: 4px; }

.report-section-title {
  font-size: 17px;
  font-weight: 700;
  margin: 24px 0 12px;
  color: var(--text);
}

.report-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}
.report-table th, .report-table td {
  border: 1px solid var(--border);
  padding: 10px 12px;
  text-align: center;
}
.report-table th { background: var(--border-light); font-weight: 600; }
.report-table td:first-child { text-align: left; font-weight: 500; }

.trend-wrap { margin-top: 4px; }
.trend-bars {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 140px;
  border-bottom: 1px solid var(--border);
  padding: 0 4px;
}
.trend-bar-col {
  flex: 1;
  min-width: 14px;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  align-items: center;
}
.trend-bar {
  width: 100%;
  background: linear-gradient(180deg, #34d399, #059669);
  border-radius: 3px 3px 0 0;
  position: relative;
  min-height: 2px;
  transition: height 0.3s;
}
.trend-bar-num {
  position: absolute;
  top: -18px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: var(--text-secondary);
  white-space: nowrap;
}
.trend-bar-day {
  font-size: 10px;
  color: var(--text-muted);
  margin-top: 4px;
  white-space: nowrap;
}
.trend-legend { font-size: 12px; color: var(--text-muted); margin-top: 8px; text-align: center; }

.report-wrong-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.rw-cell {
  text-align: center;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
}
.rw-label { font-size: 13px; color: var(--text-secondary); }
.rw-num { font-size: 26px; font-weight: 700; margin-top: 4px; }

.report-reason { display: grid; gap: 10px; margin-top: 4px; }

.report-footer-note {
  margin-top: 32px;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
  border-top: 1px dashed var(--border);
  padding-top: 12px;
}

@media (max-width: 900px) {
  .report-summary { grid-template-columns: repeat(3, 1fr); }
}

@media print {
  .noprint { display: none !important; }
  .page-body { display: block; }
  .page-content { padding: 0; }
  .report-sheet { box-shadow: none; border: none; }
  .page-root { background: #fff; }
}
</style>