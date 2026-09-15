<template>
  <div class="viz-root">
    <div class="viz-controls">
      <input class="form-input" v-model="enqVal" placeholder="值" style="width:80px" type="number" />
      <button class="btn btn-primary btn-sm" @click="queueEnqueue">Enqueue</button>
      <button class="btn btn-danger btn-sm" @click="queueDequeue">Dequeue</button>
      <button class="btn btn-outline btn-sm" @click="resetQueue">重置</button>
      <span v-if="message" class="queue-msg">{{ message }}</span>
    </div>

    <div class="viz-controls">
      <PlaybackControls
        :running="running.value"
        :paused="paused.value"
        :speed="speed.value"
        :step-mode="stepMode.value"
        :current-step="currentStep.value"
        @start="runQueueDemo"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="step"
        @reset="resetQueue"
        @update:speed="v => speed.value = v"
      />
    </div>

    <canvas ref="queueCanvas" width="500" height="140" class="viz-canvas"></canvas>
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

const queueCanvas = ref(null)
const enqVal = ref('')
const message = ref('')
let queueArr = []

onMounted(() => {
  resetQueue()
  crisp(queueCanvas.value, 500, 140)
})

const resetQueue = () => {
  stop()
  resetPlayback()
  queueArr = [8, 16, 24]
  message.value = ''
  drawQueue()
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const drawQueue = () => {
  const canvas = queueCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 500, h = 140
  const c = colors.value
  ctx.fillStyle = c.bg
  ctx.fillRect(0, 0, w, h)
  queueArr.forEach((v, i) => {
    const x = 20 + i * 58
    ctx.fillStyle = c.primaryLight
    ctx.fillRect(x, h / 2 - 20, 50, 40)
    ctx.strokeStyle = c.primary
    ctx.lineWidth = 2
    ctx.strokeRect(x, h / 2 - 20, 50, 40)
    ctx.fillStyle = c.text
    ctx.font = '14px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(v, x + 25, h / 2 + 5)
    if (i < queueArr.length - 1) {
      ctx.font = '18px sans-serif'
      ctx.fillStyle = c.edge
      ctx.fillText('→', x + 54, h / 2 + 5)
    }
  })
  if (queueArr.length > 0) {
    ctx.font = '12px sans-serif'
    ctx.fillStyle = c.danger
    ctx.fillText('front', 20, h / 2 + 45)
    ctx.fillText('rear', 20 + (queueArr.length - 1) * 58, h / 2 + 45)
  }
}

const queueEnqueue = async () => {
  const val = parseInt(enqVal.value)
  if (isNaN(val)) { message.value = '请输入有效数字'; return }
  if (queueArr.length >= 7) { message.value = '队列已满'; return }
  queueArr.push(val)
  message.value = `Enqueued ${val}`
  setDesc(`入队：${val} 从队尾（rear）入队`)
  enqVal.value = ''
  drawQueue(); await sleep()
}

const queueDequeue = async () => {
  if (queueArr.length === 0) { message.value = '队列为空'; return }
  const val = queueArr.shift()
  message.value = `Dequeued ${val}`
  setDesc(`出队：队头（front）元素 ${val} 出队`)
  drawQueue(); await sleep()
}

const runQueueDemo = async () => {
  start()
  resetQueue()
  await sleep()
  queueArr.push(32)
  message.value = 'Enqueue 32'; setDesc('入队：32 从队尾入队'); drawQueue(); await sleep()
  queueArr.push(40)
  message.value = 'Enqueue 40'; setDesc('入队：40 从队尾入队'); drawQueue(); await sleep()
  const d1 = queueArr.shift()
  message.value = `Dequeue ${d1}`; setDesc(`出队：队头 ${d1} 出队`); drawQueue(); await sleep()
  const d2 = queueArr.shift()
  message.value = `Dequeue ${d2}`; setDesc(`出队：队头 ${d2} 出队`); drawQueue(); await sleep()
  setDesc('演示结束')
  stop()
}

const runQueueStepDemo = async () => {
  startStepMode()
  resetQueue()
  await sleep()
  queueArr.push(32)
  message.value = 'Enqueue 32'; setDesc('入队：32 从队尾入队'); drawQueue(); await sleep()
  queueArr.push(40)
  message.value = 'Enqueue 40'; setDesc('入队：40 从队尾入队'); drawQueue(); await sleep()
  const d1 = queueArr.shift()
  message.value = `Dequeue ${d1}`; setDesc(`出队：队头 ${d1} 出队`); drawQueue(); await sleep()
  const d2 = queueArr.shift()
  message.value = `Dequeue ${d2}`; setDesc(`出队：队头 ${d2} 出队`); drawQueue(); await sleep()
  setDesc('演示结束')
  stop()
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    runQueueStepDemo()
  }
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.viz-canvas { border: 1px solid var(--border); border-radius: 8px; display: block; max-width: 100%; }
.queue-msg { font-size: 13px; color: var(--text-secondary); }
</style>