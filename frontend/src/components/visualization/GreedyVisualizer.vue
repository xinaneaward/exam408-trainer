<template>
  <div class="viz-root">
    <div class="viz-controls">
      <button class="btn btn-success btn-sm" @click="runGreedy">运行贪心选择</button>
      <button class="btn btn-outline btn-sm" @click="resetGreedy">重置活动</button>
      <span v-if="message" class="greedy-msg">{{ message }}</span>
    </div>

    <div class="viz-controls">
      <PlaybackControls
        :running="running.value"
        :paused="paused.value"
        :speed="speed.value"
        :step-mode="stepMode.value"
        :current-step="currentStep.value"
        @start="runGreedy"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="step"
        @reset="resetGreedy"
        @update:speed="v => speed.value = v"
      />
    </div>

    <div class="activities">
      <div
        v-for="(a, i) in activities"
        :key="i"
        class="activity-bar"
        :class="{ selected: a.selected, rejected: a.rejected }"
        :style="{ left: (a.start * 30 + 20) + 'px', width: ((a.finish - a.start) * 30) + 'px' }"
      >
        <span class="act-label">{{ a.name }}</span>
        <span class="act-time">{{ a.start }}-{{ a.finish }}</span>
      </div>
    </div>

    <div class="timeline">
      <span v-for="t in 13" :key="t" class="tick" :style="{ left: ((t - 1) * 30 + 20) + 'px' }">{{ t - 1 }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usePlayback } from '../../composables/usePlayback.js'
import PlaybackControls from './PlaybackControls.vue'

const { running, paused, speed, stepMode, currentStep, start, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc } = usePlayback(100)

const activities = ref([])
const message = ref('')

onMounted(() => {
  resetGreedy()
})

const resetGreedy = () => {
  stop()
  resetPlayback()
  message.value = ''
  activities.value = [
    { name: 'A1', start: 1, finish: 4, selected: false, rejected: false },
    { name: 'A2', start: 3, finish: 5, selected: false, rejected: false },
    { name: 'A3', start: 0, finish: 6, selected: false, rejected: false },
    { name: 'A4', start: 5, finish: 7, selected: false, rejected: false },
    { name: 'A5', start: 3, finish: 8, selected: false, rejected: false },
    { name: 'A6', start: 5, finish: 9, selected: false, rejected: false },
    { name: 'A7', start: 6, finish: 10, selected: false, rejected: false },
    { name: 'A8', start: 8, finish: 11, selected: false, rejected: false }
  ].sort((a, b) => a.finish - b.finish)
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const runGreedy = async () => {
  if (running.value) return
  start()
  activities.value.forEach(a => { a.selected = false; a.rejected = false })
  message.value = ''

  let lastFinish = -1
  const selected = []
  for (const a of activities.value) {
    if (!running.value) break
    if (a.start >= lastFinish) {
      a.selected = true
      lastFinish = a.finish
      selected.push(a.name)
      setDesc(`考察 ${a.name}[${a.start}, ${a.finish}]：start ${a.start} >= lastFinish ${lastFinish}，选择！lastFinish 更新为 ${a.finish}`)
    } else {
      a.rejected = true
      setDesc(`考察 ${a.name}[${a.start}, ${a.finish}]：start ${a.start} < lastFinish ${lastFinish}，与已选活动冲突，跳过`)
    }
    await sleep()
  }
  setDesc(`贪心选择完成，共选中 ${selected.length} 个活动`)
  message.value = `选中活动: ${selected.join(', ')}`
  stop()
}

const runStepGreedy = async () => {
  if (running.value) return
  startStepMode()
  activities.value.forEach(a => { a.selected = false; a.rejected = false })
  message.value = ''

  let lastFinish = -1
  const selected = []
  for (const a of activities.value) {
    if (!running.value) break
    if (a.start >= lastFinish) {
      a.selected = true
      lastFinish = a.finish
      selected.push(a.name)
      setDesc(`考察 ${a.name}[${a.start}, ${a.finish}]：与已选活动不冲突，选择！lastFinish 更新为 ${a.finish}`)
    } else {
      a.rejected = true
      setDesc(`考察 ${a.name}[${a.start}, ${a.finish}]：与已选活动冲突，跳过`)
    }
    await sleep()
  }
  setDesc(`贪心选择完成，共选中 ${selected.length} 个活动`)
  message.value = `选中活动: ${selected.join(', ')}`
  stop()
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    runStepGreedy()
  }
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.activities { position: relative; height: 240px; margin-top: 8px; }
.activity-bar {
  position: absolute;
  height: 34px;
  top: calc(var(--i, 0) * 28px);
  background: var(--primary-light);
  border: 1px solid var(--primary);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px;
  font-size: 12px;
  color: var(--text);
  transition: all 0.3s;
}
.activity-bar:nth-child(1) { top: 0; }
.activity-bar:nth-child(2) { top: 28px; }
.activity-bar:nth-child(3) { top: 56px; }
.activity-bar:nth-child(4) { top: 84px; }
.activity-bar:nth-child(5) { top: 112px; }
.activity-bar:nth-child(6) { top: 140px; }
.activity-bar:nth-child(7) { top: 168px; }
.activity-bar:nth-child(8) { top: 196px; }
.activity-bar.selected { background: var(--success); border-color: var(--success); color: #fff; }
.activity-bar.rejected { background: var(--bg-card); border-color: var(--border); opacity: 0.5; }
.act-label { font-weight: 600; }
.timeline { position: relative; height: 24px; border-top: 1px solid var(--border); margin-top: 4px; }
.tick { position: absolute; top: 4px; font-size: 11px; color: var(--text-secondary); }
.greedy-msg { font-size: 13px; color: var(--text-secondary); }
</style>