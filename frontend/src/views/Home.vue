<template>
  <div class="home-page">
    <TopNav :username="user?.nickname || '用户'" @toggle-theme="toggleTheme" @logout="logout" />
    
    <div class="hero-section">
      <div class="hero-content">
        <div class="hero-badge">📚 考研真题训练平台</div>
        <h1 class="hero-title">408训练系统</h1>
        <p class="hero-subtitle">收录2009-2026年全部408统考真题，支持按年份、科目、知识点筛选，一站式考研刷题平台</p>
        
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-num">{{ dash.totalQuestions || 799 }}</span>
            <span class="hero-stat-label">真题数量</span>
          </div>
          <div class="hero-stat">
            <span class="hero-stat-num">{{ dash.yearsCount || 17 }}</span>
            <span class="hero-stat-label">覆盖年份</span>
          </div>
          <div class="hero-stat">
            <span class="hero-stat-num">4</span>
            <span class="hero-stat-label">考试科目</span>
          </div>
        </div>

        <div class="hero-actions">
          <router-link to="/practice" class="hero-btn-primary">开始刷题</router-link>
          <router-link to="/overview" class="hero-btn-secondary">知识总览</router-link>
        </div>
      </div>
    </div>

    <!-- 备考看板：打卡日历 / 考试倒计时 / 今日待复习 -->
    <section class="dashboard-section">
      <div class="dashboard-container">
        <div class="dashboard-grid">
          <!-- 复习打卡日历 -->
          <div class="dash-card">
            <div class="dash-card-head">
              <span class="dash-card-title">🗓️ 复习打卡</span>
              <span class="dash-badge">{{ cal.consecutiveDays || 0 }} 天连续</span>
            </div>
            <div class="cal-head">
              <button class="cal-arrow" @click="calShift(-1)">‹</button>
              <span class="cal-month">{{ calYear }} 年 {{ calMonth }} 月</span>
              <button class="cal-arrow" @click="calShift(1)">›</button>
            </div>
            <div class="cal-week">
              <span v-for="w in ['日','一','二','三','四','五','六']" :key="w" class="cal-week-cell">{{ w }}</span>
            </div>
            <div class="cal-grid">
              <div
                v-for="(cell, i) in calCells" :key="i"
                class="cal-cell"
                :class="[cell.inMonth ? '' : 'cal-out', cell.isToday ? 'cal-today' : '']"
                :title="cell.date + (cell.count > 0 ? '：练习 ' + cell.count + ' 题' : '')"
              >
                <span class="cal-num">{{ cell.inMonth ? cell.day : '' }}</span>
                <span v-if="cell.count > 0" class="cal-dot" :class="'lv' + calLevel(cell.count)"></span>
              </div>
            </div>
            <div class="cal-legend">
              <span class="legend-label">练习量</span>
              <span class="cal-dot legend-dot lv0"></span>
              <span v-for="l in 4" :key="l" class="cal-dot legend-dot" :class="'lv' + l"></span>
            </div>
            <div class="dash-card-foot">
              本月活跃 {{ calTotalActive }} 天 · 总活跃 {{ cal.totalActiveDays || 0 }} 天
              <a class="foot-link" @click="$router.push('/report')">📄 导出月报告</a>
            </div>
          </div>

          <!-- 考试倒计时 + 阶段 -->
          <div class="dash-card countdown-card">
            <div class="dash-card-head">
              <span class="dash-card-title">⏳ 考试倒计时</span>
              <span class="phase-badge" :class="'phase-' + countdown.phase">{{ countdown.phase || '—' }}</span>
            </div>
            <div class="countdown-num-wrap">
              <span class="countdown-num">{{ countdown.daysLeft }}</span>
              <span class="countdown-unit">天</span>
            </div>
            <div class="countdown-sub">距离 {{ countdown.targetDate || '—' }} 全国硕士研究生统考</div>
            <div class="phase-progress">
              <div class="phase-bar">
                <div class="phase-fill" :style="{ width: phaseProgress + '%' }"></div>
              </div>
              <div class="phase-labels">
                <span>基础</span><span>强化</span><span>冲刺</span>
              </div>
            </div>
            <div class="phase-tip" v-if="countdown.phase === '基础'">📘 打牢基础：按科目系统过一遍教材与真题</div>
            <div class="phase-tip" v-else-if="countdown.phase === '强化'">📗 强化提高：集中刷套卷，逐题吃透错题</div>
            <div class="phase-tip shock" v-else>🔥 冲刺阶段：回归错题本+真题复刷，稳住节奏</div>
          </div>

          <!-- 今日待复习 -->
          <div class="dash-card review-card">
            <div class="dash-card-head">
              <span class="dash-card-title">📌 今日待复习</span>
              <router-link to="/wrong" class="dash-link">去错题本 →</router-link>
            </div>
            <div v-if="reviewToday.count > 0" class="review-list">
              <div v-for="(item, i) in reviewToday.items" :key="item.wrongId" class="review-item">
                <span class="review-idx">{{ i + 1 }}</span>
                <div class="review-main">
                  <div class="review-line1">
                    <span class="question-badge">{{ item.subject }}</span>
                    <span class="review-chapter">{{ item.chapter }}</span>
                  </div>
                  <div class="review-line2">
                    <span>{{ item.type }}</span>
                    <span v-if="item.year">{{ item.year }}年</span>
                    <span v-if="item.wrongCount > 0">已错 {{ item.wrongCount }} 次</span>
                  </div>
                </div>
              </div>
              <div v-if="reviewToday.items.length === 0" class="review-more">等共 {{ reviewToday.count }} 道，去错题本查看全部 →</div>
            </div>
            <div v-else class="review-empty">
              <div class="review-empty-icon">🎉</div>
              <div>今日待复习已清空，继续加油！</div>
              <router-link to="/practice" class="review-go-btn">去刷题 →</router-link>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="features-section">
      <div class="section-header">
        <h2 class="section-title">核心功能</h2>
        <p class="section-desc">全面的刷题功能，助你高效备考</p>
      </div>
      <div class="features-grid">
        <router-link to="/practice" class="feature-card">
          <div class="feature-icon">📝</div>
          <h3>真题练习</h3>
          <p>支持套卷模式和练习模式，即时反馈答案解析</p>
        </router-link>
        <router-link to="/wrong" class="feature-card">
          <div class="feature-icon">📌</div>
          <h3>错题本</h3>
          <p>自动记录错题，支持分类筛选和反复练习</p>
        </router-link>
        <router-link to="/overview" class="feature-card">
          <div class="feature-icon">🌐</div>
          <h3>知识总览</h3>
          <p>可视化知识图谱，清晰掌握各科目考点分布</p>
        </router-link>
        <router-link to="/stats" class="feature-card">
          <div class="feature-icon">📊</div>
          <h3>学习统计</h3>
          <p>详细的学习数据分析，追踪进步轨迹</p>
        </router-link>
        <router-link to="/visualization" class="feature-card">
          <div class="feature-icon">🎨</div>
          <h3>算法可视化</h3>
          <p>数据结构算法动态演示，加深理解</p>
        </router-link>
      </div>
    </section>

    <footer class="home-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <span class="footer-logo">📚 408训练系统</span>
          <p>考研计算机科目备考平台</p>
        </div>
        <div class="footer-links">
          <div class="footer-col">
            <h4>刷题</h4>
            <router-link to="/practice">真题练习</router-link>
            <router-link to="/wrong">错题本</router-link>
          </div>
          <div class="footer-col">
            <h4>资源</h4>
            <router-link to="/overview">知识图谱</router-link>
            <router-link to="/stats">学习统计</router-link>
            <router-link to="/visualization">算法可视化</router-link>
          </div>
        </div>
      </div>
      <div class="footer-bottom">
        <span>© 2026 408训练系统 | 考研计算机科目备考平台</span>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from '../components/TopNav.vue'
import api from '../api'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const dash = ref({ totalQuestions: 846, yearsCount: 18 })

const cal = ref({ days: {}, consecutiveDays: 0, totalActiveDays: 0, today: '' })
const calYear = ref(new Date().getFullYear())
const calMonth = ref(new Date().getMonth() + 1)

const countdown = ref({})
const reviewToday = ref({ count: 0, items: [] })

// 阶段进度：基础→强化→冲刺 大致按时间分布
const phaseProgress = computed(() => {
  const today = new Date(countdown.value.today || new Date().toISOString().slice(0, 10))
  const t = new Date(today)
  const target = countdown.value.targetDate ? new Date(countdown.value.targetDate) : null
  if (!target || target <= t) return 100
  const p1 = countdown.value.phase1End ? new Date(countdown.value.phase1End) : null
  const p2 = countdown.value.phase2End ? new Date(countdown.value.phase2End) : null
  if (!p1 || !p2 || p2 >= target) return 0
  const total = target - p1
  const passed = t - p1
  if (passed <= 0) return 0
  return Math.min(100, Math.max(0, Math.round((passed / total) * 100)))
})

const calCells = computed(() => {
  const y = calYear.value
  const m = calMonth.value
  const first = new Date(y, m - 1, 1)
  const startWeek = first.getDay()
  const daysInMonth = new Date(y, m, 0).getDate()
  const today = cal.value.today || new Date().toISOString().slice(0, 10)
  const cells = []
  for (let pre = startWeek - 1; pre >= 0; pre--) {
    const prev = new Date(y, m - 1, -pre)
    cells.push({ day: prev.getDate(), date: dateStr(prev), inMonth: false, isToday: false, count: 0 })
  }
  for (let d = 1; d <= daysInMonth; d++) {
    const dt = `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
    cells.push({ day: d, date: dt, inMonth: true, isToday: dt === today, count: cal.value.days?.[dt] || 0 })
  }
  let nxt = new Date(y, m - 1, daysInMonth + 1)
  while (cells.length % 7 !== 0) {
    cells.push({ day: nxt.getDate(), date: dateStr(nxt), inMonth: false, isToday: false, count: 0 })
    nxt = new Date(nxt.getFullYear(), nxt.getMonth(), nxt.getDate() + 1)
  }
  return cells
})

const calTotalActive = computed(() => {
  const days = cal.value.days || {}
  let active = 0
  const prefix = `${calYear.value}-${String(calMonth.value).padStart(2, '0')}`
  for (const [k, v] of Object.entries(days)) {
    if (k.startsWith(prefix) && v > 0) active++
  }
  return active
})

const calLevel = (count) => {
  if (count <= 0) return 0
  if (count <= 2) return 1
  if (count <= 6) return 2
  if (count <= 15) return 3
  return 4
}

const dateStr = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`

const calShift = (n) => {
  let m = calMonth.value + n
  let y = calYear.value
  if (m < 1) { m = 12; y-- }
  if (m > 12) { m = 1; y++ }
  calMonth.value = m
  calYear.value = y
}

function toggleTheme() {
  document.body.classList.toggle('dark-mode')
}

function logout() {
  try { api.logout() } catch (e) {}
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(async () => {
  try {
    const r = await api.getDashboard()
    if (r?.data?.data) {
      Object.assign(dash.value, r.data.data)
    }
  } catch (e) {}

  try {
    const r = await api.getCalendar()
    if (r?.data?.code === 200) cal.value = r.data.data || { days: {} }
  } catch (e) {}

  try {
    const r = await api.getCountdown()
    if (r?.data?.code === 200) countdown.value = r.data.data || {}
  } catch (e) {}

  try {
    const r = await api.getReviewToday()
    if (r?.data?.code === 200) reviewToday.value = r.data.data || { count: 0, items: [] }
  } catch (e) {}
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--bg);
}

.hero-section {
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  text-align: center;
}

.hero-content {
  max-width: 800px;
}

.hero-badge {
  display: inline-block;
  padding: 8px 20px;
  border-radius: 50px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 24px;
}

.hero-title {
  font-size: 52px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 16px;
  letter-spacing: -2px;
}

.hero-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.6;
  margin-bottom: 32px;
}

.hero-stats {
  display: flex;
  gap: 40px;
  justify-content: center;
  margin-bottom: 40px;
}

.hero-stat {
  text-align: center;
}

.hero-stat-num {
  display: block;
  font-size: 36px;
  font-weight: 700;
  color: #fff;
}

.hero-stat-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
}

.hero-btn-primary {
  padding: 14px 32px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  text-decoration: none;
  background: #fff;
  color: #667eea;
  transition: all 0.2s;
}

.hero-btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.hero-btn-secondary {
  padding: 14px 32px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  text-decoration: none;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  border: 2px solid rgba(255, 255, 255, 0.3);
  transition: all 0.2s;
}

.hero-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.25);
  transform: translateY(-3px);
}

/* ===== 备考看板 ===== */
.dashboard-section {
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 20px;
}

.dash-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 20px;
  box-shadow: var(--shadow-sm);
}

.dash-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.dash-card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text);
}

.dash-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(5, 150, 105, 0.12);
  color: var(--success);
}

.cal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.cal-month {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.cal-arrow {
  width: 26px;
  height: 26px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
}
.cal-arrow:hover { border-color: var(--primary); color: var(--primary); }

.cal-week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 4px;
}

.cal-week-cell {
  text-align: center;
  font-size: 11px;
  color: var(--text-muted);
  padding: 4px 0;
}

.cal-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.cal-cell {
  position: relative;
  height: 34px;
  border-radius: 7px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: default;
}

.cal-cell.cal-out {
  opacity: 0.35;
}

.cal-num {
  font-size: 12px;
  color: var(--text);
}

.cal-today {
  background: rgba(102, 126, 234, 0.15);
  box-shadow: inset 0 0 0 2px #667eea;
}

.cal-dot {
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: transparent;
}
.cal-dot.lv1 { background: #c7e8d6; }
.cal-dot.lv2 { background: #7cc9a4; }
.cal-dot.lv3 { background: #34a571; }
.cal-dot.lv4 { background: #0d6b46; }

.cal-legend {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 10px;
  font-size: 11px;
  color: var(--text-muted);
}
.legend-label { margin-right: 4px; }
.legend-dot { position: static; transform: none; }
.legend-dot.lv0 { background: var(--border); }

.dash-card-foot {
  margin-top: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.foot-link {
  color: var(--primary);
  cursor: pointer;
  text-decoration: none;
}
.foot-link:hover { text-decoration: underline; }

/* 倒计时 */
.countdown-card {
  background: linear-gradient(160deg, #667eea 0%, #764ba2 100%);
  border-color: transparent;
  color: #fff;
}
.countdown-card .dash-card-title { color: #fff; }
.phase-badge {
  font-size: 13px;
  font-weight: 700;
  padding: 4px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
}
.phase-badge.phase-基础 { background: rgba(96, 165, 250, 0.35); }
.phase-badge.phase-强化 { background: rgba(251, 191, 36, 0.35); }
.phase-badge.phase-冲刺 { background: rgba(248, 113, 113, 0.4); }

.countdown-num-wrap {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 10px 0 4px;
}
.countdown-num {
  font-size: 56px;
  font-weight: 800;
  color: #fff;
  letter-spacing: -2px;
  line-height: 1;
}
.countdown-unit { font-size: 18px; color: rgba(255, 255, 255, 0.85); }
.countdown-sub { font-size: 13px; color: rgba(255, 255, 255, 0.75); margin-bottom: 14px; }

.phase-progress { margin: 10px 0; }
.phase-bar {
  height: 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.25);
  overflow: hidden;
}
.phase-fill {
  height: 100%;
  border-radius: 4px;
  background: #fff;
  transition: width 0.5s;
}
.phase-labels {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 4px;
}

.phase-tip {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.95);
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.15);
  line-height: 1.5;
}
.phase-tip.shock { font-weight: 600; }

/* 今日待复习 */
.review-card .dash-link {
  font-size: 13px;
  color: var(--primary);
  text-decoration: none;
  font-weight: 500;
}
.review-list {
  max-height: 320px;
  overflow-y: auto;
}
.review-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
}
.review-item:last-child { border-bottom: none; }
.review-idx {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: rgba(246, 87, 87, 0.12);
  color: #f65b5b;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.review-main { flex: 1; min-width: 0; }
.review-line1 { display: flex; align-items: center; gap: 6px; margin-bottom: 3px; }
.review-chapter { font-size: 13px; font-weight: 600; color: var(--text); }
.review-line2 { font-size: 12px; color: var(--text-secondary); display: flex; gap: 8px; }
.review-more { font-size: 13px; color: var(--text-secondary); padding: 10px 0; text-align: center; }

.review-empty {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  padding: 32px 0;
}
.review-empty-icon { font-size: 40px; margin-bottom: 10px; }
.review-go-btn {
  display: inline-block;
  margin-top: 14px;
  padding: 8px 20px;
  border-radius: 10px;
  background: var(--primary);
  color: #fff;
  font-size: 14px;
  text-decoration: none;
}

/* ===== 核心功能 ===== */
.features-section {
  padding: 80px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--text);
  margin-bottom: 12px;
}

.section-desc {
  font-size: 16px;
  color: var(--text-secondary);
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 32px;
  text-decoration: none;
  color: var(--text);
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: var(--shadow-lg);
  border-color: var(--primary);
}

.feature-icon {
  font-size: 40px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 10px;
}

.feature-card p {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.home-footer {
  background: var(--bg-card);
  border-top: 1px solid var(--border);
  margin-top: 40px;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
}

.footer-brand p {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 8px;
}

.footer-logo {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
}

.footer-links {
  display: flex;
  gap: 40px;
  justify-content: flex-end;
}

.footer-col h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text);
}

.footer-col a {
  display: block;
  font-size: 13px;
  color: var(--text-muted);
  text-decoration: none;
  margin-bottom: 6px;
}

.footer-col a:hover {
  color: var(--primary);
}

.footer-bottom {
  border-top: 1px solid var(--border);
  padding: 16px 20px;
  text-align: center;
  font-size: 13px;
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .features-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .hero-title {
    font-size: 36px;
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
  .hero-stats {
    gap: 20px;
  }
  .hero-stat-num {
    font-size: 28px;
  }
}
</style>