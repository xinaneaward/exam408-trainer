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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from '../components/TopNav.vue'
import api from '../api'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const dash = ref({ totalQuestions: 846, yearsCount: 18 })

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
