<template>
  <div class="auth-page">
    <div class="auth-card">
      <h3>⊛ 注册账号</h3>
      <p class="subtitle">创建账号开始刷题之旅</p>
      <div class="error" v-if="errorMsg">{{ errorMsg }}</div>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input class="form-input" type="text" ref="usernameRef" required autocomplete="username" />
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input class="form-input" type="password" ref="passwordRef" required autocomplete="new-password" />
        </div>
        <div class="form-group">
          <label class="form-label">确认密码</label>
          <input class="form-input" type="password" ref="confirmRef" required autocomplete="new-password" />
        </div>
        <div class="form-group">
          <label class="form-label">昵称</label>
          <input class="form-input" type="text" ref="nicknameRef" placeholder="选填" />
        </div>
        <button class="btn btn-primary btn-block btn-lg" type="submit" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>
      </form>
      <p style="text-align:center; margin-top:16px; font-size:13px;">
        已有账号？<router-link to="/login">立即登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const router = useRouter()

const usernameRef = ref(null)
const passwordRef = ref(null)
const confirmRef = ref(null)
const nicknameRef = ref(null)
const errorMsg = ref('')
const loading = ref(false)

async function handleRegister() {
  errorMsg.value = ''
  const username = usernameRef.value.value.trim()
  const password = passwordRef.value.value
  const confirm = confirmRef.value.value
  const nickname = nicknameRef.value.value.trim() || username

  if (!username || !password) {
    errorMsg.value = '请填写用户名和密码'
    return
  }
  if (password.length < 6) {
    errorMsg.value = '密码长度至少6位'
    return
  }
  if (password !== confirm) {
    errorMsg.value = '两次输入的密码不一致'
    return
  }

  loading.value = true
  try {
    const r = await api.register({ username, password, nickname })
    if (r.data.code === 200) {
      localStorage.setItem('user', JSON.stringify(r.data.data))
      router.push('/')
    } else {
      errorMsg.value = r.data.message || '注册失败'
    }
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '网络错误，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.auth-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.auth-card h3 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #1a73e8;
  text-align: center;
}

.subtitle {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin: 0 0 24px;
}

.error {
  background: #ffebee;
  color: #c62828;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  margin-bottom: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 15px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-block {
  width: 100%;
}

.btn-lg {
  padding: 14px 24px;
  font-size: 16px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
