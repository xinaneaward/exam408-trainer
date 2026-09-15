import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 全局 AI 上下文：共享当前题目信息给悬浮球助手
 * Practice/Wrong 页面在打开 AI 讲解或切换题目时设置
 */
export const useAiContextStore = defineStore('aiContext', () => {
  // 当前题目上下文（可选）：路由感知时注入
  const currentQuestion = ref(null)

  function setQuestion(q) {
    currentQuestion.value = q ? { ...q } : null
  }

  function clear() {
    currentQuestion.value = null
  }

  return { currentQuestion, setQuestion, clear }
})
