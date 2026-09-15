<template>
  <div class="algo-list-page">
    <TopNav :username="user?.nickname || '用户'" @toggle-theme="toggleTheme" @logout="logout" />
    <div class="page-content">
      <div class="page-header">
        <h2>🎨 算法可视化</h2>
        <p class="page-subtitle">408 统考考研大纲算法分类演示，点击任意算法查看动态可视化</p>
      </div>

      <div class="search-bar">
        <input v-model="keyword" class="form-input" placeholder="搜索算法名称或关键词..." />
      </div>

      <div v-for="cat in filteredCategories" :key="cat.key" class="category-section">
        <div class="category-header">
          <span class="category-icon">{{ cat.icon }}</span>
          <div>
            <h3 class="category-name">{{ cat.name }}</h3>
            <p class="category-desc">{{ cat.desc }}</p>
          </div>
        </div>
        <div class="algo-grid">
          <div
            v-for="algo in cat.algorithms"
            :key="algo.key"
            class="algo-card"
            :class="{ disabled: false, [difficultyClass(algo.difficulty)]: true }"
            @click="goToAlgo(algo)"
          >
            <div class="algo-top">
              <span class="algo-difficulty" :class="difficultyClass(algo.difficulty)">{{ algo.difficulty }}</span>
            </div>
            <h4 class="algo-name">{{ algo.name }}</h4>
            <p class="algo-desc">{{ algo.desc }}</p>
            <div class="algo-meta">
              <span>⏱ {{ algo.time }}</span>
              <span>💾 {{ algo.space }}</span>
            </div>
            <div class="algo-arrow">→</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from '../../components/TopNav.vue'
import api from '../../api'
import { categories, algorithms } from '../../data/algorithms.js'

const router = useRouter()
const user = ref(null)
const keyword = ref('')

onMounted(() => {
  const raw = localStorage.getItem('user')
  user.value = raw ? JSON.parse(raw) : null
})

const filteredCategories = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return categories.map(cat => {
    const list = algorithms.filter(a => a.category === cat.key)
    const filtered = kw
      ? list.filter(a => a.name.toLowerCase().includes(kw) || a.desc.toLowerCase().includes(kw))
      : list
    return { ...cat, algorithms: filtered }
  }).filter(cat => cat.algorithms.length > 0)
})

const difficultyClass = (d) => {
  if (d === '简单') return 'easy'
  if (d === '中等') return 'medium'
  return 'hard'
}

const goToAlgo = (algo) => {
  router.push(`/visualization/${algo.category}/${algo.key}`)
}

const toggleTheme = () => {
  document.body.classList.toggle('dark-mode')
}

const logout = async () => {
  try { await api.logout() } catch (e) { /* ignore */ }
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.algo-list-page { min-height: 100vh; background: var(--bg); }
.page-content { max-width: 1200px; margin: 0 auto; padding: 24px 20px; }
.page-header { margin-bottom: 20px; }
.page-header h2 { font-size: 24px; font-weight: 700; color: var(--text); }
.page-subtitle { font-size: 14px; color: var(--text-secondary); margin-top: 6px; }

.search-bar { margin-bottom: 24px; }
.search-bar .form-input { max-width: 400px; }

.category-section { margin-bottom: 32px; }
.category-header { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; padding: 12px 16px; background: rgba(5, 150, 105, 0.06); border: 2px solid var(--primary); border-radius: 10px; }
.category-icon { font-size: 28px; }
.category-name { font-size: 18px; font-weight: 700; color: var(--text); }
.category-desc { font-size: 13px; color: var(--text-secondary); margin-top: 2px; }

.algo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }
.algo-card {
  background: var(--bg-card);
  border: 2px solid var(--border);
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}
.algo-card:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
}
.algo-card.disabled { opacity: 0.6; cursor: not-allowed; }
.algo-card.easy { border-color: var(--success); background: rgba(5, 150, 105, 0.06); }
.algo-card.medium { border-color: var(--warning); background: rgba(245, 158, 11, 0.06); }
.algo-card.hard { border-color: var(--danger); background: rgba(239, 68, 68, 0.06); }
.algo-card.easy:hover { border-color: var(--success); background: rgba(5, 150, 105, 0.1); box-shadow: 0 4px 16px rgba(5, 150, 105, 0.15); }
.algo-card.medium:hover { border-color: var(--warning); background: rgba(245, 158, 11, 0.1); box-shadow: 0 4px 16px rgba(245, 158, 11, 0.15); }
.algo-card.hard:hover { border-color: var(--danger); background: rgba(239, 68, 68, 0.1); box-shadow: 0 4px 16px rgba(239, 68, 68, 0.15); }
.algo-top { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.algo-difficulty { font-size: 11px; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
.algo-difficulty.easy { background: rgba(5, 150, 105, 0.12); color: var(--success); }
.algo-difficulty.medium { background: rgba(245, 158, 11, 0.12); color: var(--warning); }
.algo-difficulty.hard { background: rgba(239, 68, 68, 0.12); color: var(--danger); }
.algo-name { font-size: 16px; font-weight: 700; color: var(--text); margin-bottom: 6px; }
.algo-desc { font-size: 13px; color: var(--text-secondary); line-height: 1.5; margin-bottom: 12px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.algo-meta { display: flex; gap: 12px; font-size: 12px; color: var(--text-muted); }
.algo-arrow { position: absolute; right: 12px; bottom: 8px; font-size: 20px; color: var(--primary); opacity: 0; transition: opacity 0.2s; }
.algo-card:hover .algo-arrow { opacity: 1; }
</style>
