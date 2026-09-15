<template>
  <div class="playback-controls">
    <button class="btn btn-success btn-sm" :disabled="running" @click="$emit('start')">▶ 自动运行</button>
    <button class="btn btn-warning btn-sm" @click="$emit('switchtostepmode')">
      {{ stepMode ? '⏸ 退出分步' : (running ? '▶ 切换分步' : '▶ 分步模式') }}
    </button>
    <button v-if="running" class="btn btn-info btn-sm" @click="$emit('pause')">{{ paused ? '▶ 继续' : '⏸ 暂停' }}</button>
    <button v-if="running && stepMode" class="btn btn-primary btn-sm" @click="$emit('step')">▶ 下一步</button>
    <button class="btn btn-outline btn-sm" @click="$emit('reset')">⟲ 重置</button>
    <label class="form-label" style="margin:0">速度</label>
    <input type="range" :value="speed" min="10" max="300" @input="e => $emit('update:speed', Number(e.target.value))" />
    <span class="speed-label">{{ speed }}ms</span>
    <span v-if="stepMode" class="mode-label">分步模式</span>
    <span v-if="currentStep > 0" class="step-label">步骤: {{ currentStep }}</span>

    <div class="playback-status" :class="{ active: live.active }">
      <template v-if="live.active">
        <span class="status-dot"></span>
        <span class="status-op">{{ live.desc || '就绪，点击箭头按钮开始执行…' }}</span>
        <span v-if="live.step > 0" class="status-step">第 {{ live.step }} 步</span>
      </template>
      <template v-else>
        <span class="status-op muted">就绪 —— 点击「自动运行」或「分步模式」开始演示</span>
      </template>
    </div>
  </div>
</template>

<script setup>
import { vizLive } from '../../composables/vizLive.js'

defineProps({
  running: { type: Boolean, default: false },
  paused: { type: Boolean, default: false },
  speed: { type: Number, default: 50 },
  stepMode: { type: Boolean, default: false },
  currentStep: { type: Number, default: 0 }
})
defineEmits(['start', 'switchtostepmode', 'pause', 'step', 'reset', 'update:speed'])

const live = vizLive
</script>

<style scoped>
.playback-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.speed-label { font-size: 13px; color: var(--text-secondary); min-width: 42px; }
.mode-label { font-size: 12px; color: var(--warning); font-weight: 600; }
.step-label { font-size: 12px; color: var(--primary); font-weight: 600; }

.playback-status {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 8px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  font-size: 13px;
  color: var(--text);
  min-height: 32px;
}
.playback-status .status-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); flex-shrink: 0; }
.playback-status .status-op { flex: 1; }
.playback-status .status-op.muted { color: var(--text-secondary); }
.playback-status .status-step { font-weight: 600; color: var(--primary); flex-shrink: 0; }
</style>