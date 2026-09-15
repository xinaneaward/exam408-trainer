<template>
  <div class="page-root" :class="theme">
    <TopNav :username="user?.nickname||'用户'" @toggle-theme="toggleTheme" @logout="logout" />
    <div class="page-body">
      <main class="page-content">
      <div class="page-header">
        <h2>PDF原题浏览</h2>
      </div>

      <div class="card">
        <div class="card-title">选择年份</div>
        <div style="display:flex; flex-wrap:wrap; gap:6px;">
          <button
            v-for="y in years"
            :key="y"
            class="btn"
            :class="selectedYear === y ? 'btn-primary' : 'btn-outline'"
            @click="selectYear(y)"
          >
            {{ y }}
          </button>
        </div>
      </div>

      <div class="card">
        <div style="display:flex; align-items:center; gap:12px; flex-wrap:wrap;">
          <label class="form-label" style="margin-bottom:0;">
            来源类型：
            <select class="form-input" v-model="sourceType" @change="onSourceChange">
              <option value="exam">真题</option>
              <option value="analysis">真题解析</option>
            </select>
          </label>
        </div>
      </div>

      <div class="card" style="text-align:center;">
        <div style="min-height:400px; display:flex; align-items:center; justify-content:center; background:#f0f0f0; border-radius:8px; margin-bottom:12px; position:relative;">
          <img
            v-if="!imgError"
            :src="currentImgSrc"
            :alt="selectedYear + '年第' + currentPage + '页'"
            style="max-width:100%; max-height:70vh;"
            @error="onImgError"
          />
          <div v-else style="color:var(--text-secondary); padding:40px;">
            图片加载失败，可能该页不存在
          </div>
        </div>

        <div style="display:flex; align-items:center; justify-content:center; gap:16px;">
          <button class="btn btn-outline" @click="prevPage" :disabled="currentPage <= 1">上一页</button>
          <span style="font-size:14px; font-weight:500;">
            {{ selectedYear }}年 · {{ sourceType === 'exam' ? '真题' : '真题解析' }} · 第{{ currentPage }}页
          </span>
          <button class="btn btn-outline" @click="nextPage">下一页</button>
        </div>
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

const user = ref(null)
const selectedYear = ref(2025)
const sourceType = ref('exam')
const currentPage = ref(1)
const imgError = ref(false)
const theme = ref(localStorage.getItem('theme') || 'light')

function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('theme', theme.value)
}

const years = []
for (let y = 2009; y <= 2025; y++) {
  years.push(y)
}

const currentImgSrc = computed(() => {
  const type = sourceType.value
  const year = selectedYear.value
  const page = String(currentPage.value).padStart(2, '0')
  return `/images/pages/${type}_${year}_p${page}.png`
})

const selectYear = (y) => {
  selectedYear.value = y
  currentPage.value = 1
  imgError.value = false
}

const onSourceChange = () => {
  currentPage.value = 1
  imgError.value = false
}

const prevPage = () => {
  if (currentPage.value > 1) {
    currentPage.value--
    imgError.value = false
  }
}

const nextPage = () => {
  currentPage.value++
  imgError.value = false
}

const onImgError = () => {
  imgError.value = true
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

onMounted(() => {
  const raw = localStorage.getItem('user')
  user.value = raw ? JSON.parse(raw) : null
})
</script>

<style scoped>
.page-root { min-height:100vh; }
.page-body { display:flex; }
.page-content { flex:1; padding:20px; }
</style>
