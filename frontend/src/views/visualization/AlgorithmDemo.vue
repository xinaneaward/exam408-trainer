<template>
  <div class="demo-page">
    <TopNav :username="user?.nickname || '用户'" @toggle-theme="toggleTheme" @logout="logout" />
    <div class="page-content">
      <div class="page-header">
        <button class="btn btn-outline btn-sm" @click="goBack">← 返回算法列表</button>
        <div class="header-info">
          <div class="breadcrumb">{{ algo?.categoryName }} / {{ algo?.name }}</div>
          <h2 class="page-title">{{ algo?.name }}</h2>
          <p class="page-desc">{{ algo?.desc }}</p>
        </div>
      </div>

      <div v-if="!algo" class="not-found">
        <div class="not-found-icon">🔍</div>
        <h3>算法未找到</h3>
        <p>无法找到指定的算法，请返回列表重新选择</p>
        <button class="btn btn-primary" @click="goBack">返回算法列表</button>
      </div>

      <div v-else class="demo-main">
        <div class="visualizer-card card">
          <div class="card-title">🎨 算法演示</div>
          <component :is="VisualizerComponent" :algo-key="algo.key" />
        </div>

        <AlgorithmPanel :algo="algo" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TopNav from '../../components/TopNav.vue'
import AlgorithmPanel from '../../components/visualization/AlgorithmPanel.vue'
import SortVisualizer from '../../components/visualization/SortVisualizer.vue'
import SearchVisualizer from '../../components/visualization/SearchVisualizer.vue'
import TreeVisualizer from '../../components/visualization/TreeVisualizer.vue'
import GraphVisualizer from '../../components/visualization/GraphVisualizer.vue'
import LinkedListVisualizer from '../../components/visualization/LinkedListVisualizer.vue'
import StackVisualizer from '../../components/visualization/StackVisualizer.vue'
import QueueVisualizer from '../../components/visualization/QueueVisualizer.vue'
import KmpVisualizer from '../../components/visualization/KmpVisualizer.vue'
import { findAlgorithm, categories } from '../../data/algorithms.js'
import api from '../../api'

const router = useRouter()
const route = useRoute()
const user = ref(null)

const componentMap = {
  SortVisualizer,
  SearchVisualizer,
  TreeVisualizer,
  GraphVisualizer,
  LinkedListVisualizer,
  StackVisualizer,
  QueueVisualizer,
  KmpVisualizer
}

const algo = computed(() => {
  const category = route.params.category
  const algoKey = route.params.algoKey
  const found = findAlgorithm(category, algoKey)
  if (!found) return null
  const cat = categories.find(c => c.key === category)
  return { ...found, categoryName: cat?.name || category }
})

const VisualizerComponent = computed(() => {
  if (!algo.value) return null
  return componentMap[algo.value.component] || null
})

const goBack = () => {
  router.push('/visualization')
}

const toggleTheme = () => {
  document.body.classList.toggle('dark-mode')
}

const logout = async () => {
  try { await api.logout() } catch (e) { }
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(() => {
  const raw = localStorage.getItem('user')
  user.value = raw ? JSON.parse(raw) : null
})
</script>

<style scoped>
.demo-page { min-height: 100vh; background: var(--bg); }
.page-content { max-width: 900px; margin: 0 auto; padding: 24px 20px; }

.page-header { display: flex; gap: 16px; align-items: flex-start; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--border); }
.page-header .btn { margin-top: 4px; }

.header-info { flex: 1; }
.breadcrumb { font-size: 13px; color: var(--text-muted); margin-bottom: 4px; }
.page-title { font-size: 24px; font-weight: 700; color: var(--text); margin-bottom: 6px; }
.page-desc { font-size: 14px; color: var(--text-secondary); }

.not-found { text-align: center; padding: 60px 20px; }
.not-found-icon { font-size: 56px; margin-bottom: 16px; }
.not-found h3 { font-size: 20px; font-weight: 600; color: var(--text); margin-bottom: 8px; }
.not-found p { font-size: 14px; color: var(--text-secondary); margin-bottom: 20px; }

.demo-main { display: flex; flex-direction: column; gap: 16px; }

.visualizer-card { padding: 20px; background: rgba(5, 150, 105, 0.06); border: 2px solid var(--primary); }
.visualizer-card .card-title { margin-bottom: 16px; font-size: 16px; font-weight: 600; }
</style>