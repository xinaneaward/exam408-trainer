<template>
  <div class="viz-root">
    <div class="viz-controls">
      <input class="form-input" v-model="pushVal" placeholder="值" style="width:80px" type="number" />
      <button class="btn btn-primary btn-sm" @click="stackPush">Push</button>
      <button class="btn btn-danger btn-sm" @click="stackPop">Pop</button>
      <button class="btn btn-outline btn-sm" @click="resetStack">重置</button>
      <span v-if="message" class="stack-msg">{{ message }}</span>
    </div>

    <div class="viz-controls">
      <PlaybackControls
        :running="running.value"
        :paused="paused.value"
        :speed="speed.value"
        :step-mode="stepMode.value"
        :current-step="currentStep.value"
        @start="runStackDemo"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="step"
        @reset="resetStack"
        @update:speed="v => speed.value = v"
      />
    </div>

    <canvas ref="stackCanvas" width="220" height="300" class="viz-canvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useVisualizationTheme } from '../../composables/useVisualizationTheme.js'
import { usePlayback } from '../../composables/usePlayback.js'
import { useCrispCanvas } from '../../composables/useCrispCanvas.js'
import PlaybackControls from './PlaybackControls.vue'

const { colors } = useVisualizationTheme()
const { running, paused, speed, stepMode, currentStep, start, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc } = usePlayback(100)
const { crisp } = useCrispCanvas()

const stackCanvas = ref(null)
const pushVal = ref('')
const message = ref('')
let stackArr = []

onMounted(() => {
  resetStack()
  crisp(stackCanvas.value, 220, 300)
})

const resetStack = () => {
  stop()
  resetPlayback()
  stackArr = [10, 22, 35]
  message.value = ''
  drawStack()
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const drawStack = () => {
  const canvas = stackCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 220, h = 300
  const c = colors.value
  ctx.fillStyle = c.bg
  ctx.fillRect(0, 0, w, h)
  ctx.strokeStyle = c.primary
  ctx.lineWidth = 2
  ctx.strokeRect(w / 2 - 40, 20, 80, 250)
  stackArr.forEach((v, i) => {
    const y = h - 40 - (i + 1) * 34
    ctx.fillStyle = c.primaryLight
    ctx.fillRect(w / 2 - 34, y, 68, 28)
    ctx.strokeStyle = c.primary
    ctx.strokeRect(w / 2 - 34, y, 68, 28)
    ctx.fillStyle = c.text
    ctx.font = '13px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(v, w / 2, y + 19)
  })
  if (stackArr.length > 0) {
    ctx.fillStyle = c.danger
    ctx.font = '12px sans-serif'
    ctx.fillText('← top', w / 2 + 55, h - 40 - stackArr.length * 34 + 19)
  }
}

const stackPush = async () => {
  const val = parseInt(pushVal.value)
  if (isNaN(val)) { message.value = '请输入有效数字'; return }
  if (stackArr.length >= 6) { message.value = '栈已满'; return }
  stackArr.push(val)
  message.value = `Pushed ${val}`
  setDesc(`入栈：${val} 压入栈顶，top 上移`)
  pushVal.value = ''
  drawStack(); await sleep()
}

const stackPop = async () => {
  if (stackArr.length === 0) { message.value = '栈为空'; return }
  const val = stackArr.pop()
  message.value = `Popped ${val}`
  setDesc(`出栈：顶部元素 ${val} 弹出，top 下移`)
  drawStack(); await sleep()
}

const runStackDemo = async () => {
  start()
  resetStack()
  await sleep()
  stackArr.push(48)
  message.value = 'Push 48'; setDesc('入栈：48 压入栈顶'); drawStack(); await sleep()
  stackArr.push(61)
  message.value = 'Push 61'; setDesc('入栈：61 压入栈顶'); drawStack(); await sleep()
  const p1 = stackArr.pop()
  message.value = `Pop ${p1}`; setDesc(`出栈：${p1} 弹出`); drawStack(); await sleep()
  const p2 = stackArr.pop()
  message.value = `Pop ${p2}`; setDesc(`出栈：${p2} 弹出`); drawStack(); await sleep()
  setDesc('演示结束')
  stop()
}

const runStackStepDemo = async () => {
  startStepMode()
  resetStack()
  await sleep()
  stackArr.push(48)
  message.value = 'Push 48'; setDesc('入栈：48 压入栈顶'); drawStack(); await sleep()
  stackArr.push(61)
  message.value = 'Push 61'; setDesc('入栈：61 压入栈顶'); drawStack(); await sleep()
  const p1 = stackArr.pop()
  message.value = `Pop ${p1}`; setDesc(`出栈：${p1} 弹出`); drawStack(); await sleep()
  const p2 = stackArr.pop()
  message.value = `Pop ${p2}`; setDesc(`出栈：${p2} 弹出`); drawStack(); await sleep()
  setDesc('演示结束')
  stop()
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    runStackStepDemo()
  }
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.viz-canvas { border: 1px solid var(--border); border-radius: 8px; display: block; }
.stack-msg { font-size: 13px; color: var(--text-secondary); }
</style>