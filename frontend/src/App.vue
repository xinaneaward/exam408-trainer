<template>
  <router-view />
  <AiAssistant v-if="isLoggedIn" />
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from './api'
import AiAssistant from './components/ai/AiAssistant.vue'

const route = useRoute()
const isLoggedIn = ref(!!localStorage.getItem('user'))
// 路由变化时重新检查登录态（localStorage 非响应式，需手动刷新）
watch(() => route.path, () => { isLoggedIn.value = !!localStorage.getItem('user') })

onMounted(async () => {
  const savedTheme = localStorage.getItem('theme')
  if (savedTheme === 'dark' || (!savedTheme && window.matchMedia?.('(prefers-color-scheme: dark)').matches)) {
    document.body.classList.add('dark-mode')
  } else {
    document.body.classList.remove('dark-mode')
  }

  const cached = localStorage.getItem('user')
  if (!cached) return
  try {
    const r = await api.getUser()
    if (r.data.code === 200) {
      localStorage.setItem('user', JSON.stringify(r.data.data))
    }
  } catch (e) {
    localStorage.removeItem('user')
  }
})
</script>
