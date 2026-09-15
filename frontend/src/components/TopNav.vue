<template>
  <div class="tn-root">
    <div class="tn-left">
      <router-link to="/" class="tn-logo">
        <span class="tn-logo-icon">📚</span>
        <span class="tn-logo-text">408训练系统</span>
      </router-link>
    </div>
    <nav class="tn-nav">
      <router-link to="/" class="tn-link">首页</router-link>
      <router-link to="/overview" class="tn-link">知识总览</router-link>
      <router-link to="/practice" class="tn-link">刷题练习</router-link>
      <router-link to="/wrong" class="tn-link">错题本</router-link>
      <router-link to="/stats" class="tn-link">学习统计</router-link>
      <router-link to="/visualization" class="tn-link">算法可视化</router-link>
    </nav>
    <div class="tn-right">
      <button class="tn-btn tn-theme-btn" @click="$emit('toggleTheme')" title="切换主题">
        <span>🌓</span>
      </button>
      <span class="tn-user">👤 {{ username }}</span>
      <button class="tn-btn tn-logout" @click="$emit('logout')">退出</button>
    </div>
  </div>
</template>

<script setup>
defineProps({ username: { type: String, default: '用户' } })
defineEmits(['toggleTheme', 'logout'])
</script>

<style scoped>
.tn-root {
  display: flex;
  align-items: center;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  padding: 0 20px;
  gap: 8px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  position: sticky;
  top: 0;
  z-index: 1000;
}
.tn-left { display: flex; align-items: center; gap: 12px; min-width: 140px; }
.tn-logo {
  display: flex;
  align-items: center;
  gap: 6px;
  text-decoration: none;
  white-space: nowrap;
}
.tn-logo-icon {
  font-size: 22px;
}
.tn-logo-text {
  font-size: 17px;
  font-weight: 700;
  color: #1a73e8;
  letter-spacing: 0.5px;
}
.tn-nav { display: flex; gap: 2px; flex: 1; justify-content: center; }
.tn-link {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  color: #5f6368;
  text-decoration: none;
  font-weight: 500;
  position: relative;
}
.tn-link:hover { 
  background: rgba(26, 115, 232, 0.1); 
  color: #1a73e8; 
}
.tn-link.active, .tn-link.router-link-exact-active {
  color: #1a73e8;
  font-weight: 600;
  background: rgba(26, 115, 232, 0.1);
}
.tn-right { display: flex; align-items: center; gap: 12px; }
.tn-btn {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 16px;
  padding: 6px 10px;
  border-radius: 8px;
  color: #5f6368;
}
.tn-btn:hover { 
  background: rgba(26, 115, 232, 0.1); 
  color: #1a73e8; 
}
.tn-theme-btn { font-size: 18px; }
.tn-logout {
  font-size: 13px;
  color: #5f6368;
  padding: 4px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
}
.tn-logout:hover { 
  border-color: #e53935; 
  color: #e53935; 
  background: rgba(239,68,68,0.05); 
}
.tn-user {
  font-size: 13px;
  color: #5f6368;
  white-space: nowrap;
}
@media (max-width: 1024px) {
  .tn-nav .tn-link { padding: 6px 10px; font-size: 13px; }
}
@media (max-width: 768px) {
  .tn-nav { display: none; }
}
</style>
