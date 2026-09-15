<template>
  <div class="viz-root">
    <div class="viz-controls">
      <input class="form-input" v-model.number="target" placeholder="目标值" style="width:90px" type="number" />
      <button class="btn btn-success btn-sm" @click="runSearch">开始查找</button>
      <button class="btn btn-outline btn-sm" @click="resetSearch">重置</button>
      <span v-if="message" class="search-msg">{{ message }}</span>
    </div>

    <div class="viz-controls">
      <PlaybackControls
        :running="running.value"
        :paused="paused.value"
        :speed="speed.value"
        :step-mode="stepMode.value"
        :current-step="currentStep.value"
        @start="runSearch"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="step"
        @reset="resetSearch"
        @update:speed="v => speed.value = v"
      />
    </div>

    <canvas ref="searchCanvas" width="800" height="220" class="viz-canvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useVisualizationTheme } from '../../composables/useVisualizationTheme.js'
import { usePlayback } from '../../composables/usePlayback.js'
import { useCrispCanvas } from '../../composables/useCrispCanvas.js'
import PlaybackControls from './PlaybackControls.vue'

const props = defineProps({
  algoKey: { type: String, default: 'sequential' }
})

const { colors } = useVisualizationTheme()
const { running, paused, speed, stepMode, currentStep, start, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc, setLine } = usePlayback(100)
const { crisp } = useCrispCanvas()

const searchCanvas = ref(null)
const target = ref('')
const message = ref('')

let data = []
let highlightIdx = -1
let rangeLow = -1, rangeHigh = -1

onMounted(() => {
  resetSearch()
  crisp(searchCanvas.value, 800, 220)
})

watch(() => props.algoKey, () => {
  resetSearch()
})

const resetSearch = () => {
  stop()
  resetPlayback()
  highlightIdx = -1
  rangeLow = -1; rangeHigh = -1
  message.value = ''
  if (props.algoKey === 'binary') {
    data = Array.from({ length: 16 }, (_, i) => (i + 1) * 5 + Math.floor(Math.random() * 3))
    data.sort((a, b) => a - b)
  } else if (props.algoKey === 'hash') {
    data = Array.from({ length: 12 }, () => Math.floor(Math.random() * 90) + 10)
  } else {
    data = Array.from({ length: 16 }, () => Math.floor(Math.random() * 90) + 10)
  }
  draw()
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const draw = () => {
  const canvas = searchCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 800, h = 220
  const c = colors.value
  ctx.fillStyle = c.bg
  ctx.fillRect(0, 0, w, h)

  if (props.algoKey === 'hash') {
    drawHash(ctx)
    return
  }

  const n = data.length
  const cellW = Math.floor((w - 40) / n)
  const startX = (w - n * cellW) / 2
  data.forEach((v, i) => {
    const x = startX + i * cellW
    const y = 60
    if (i === highlightIdx) ctx.fillStyle = c.warning
    else if (i >= rangeLow && i <= rangeHigh) ctx.fillStyle = c.primaryLight
    else ctx.fillStyle = c.cardBg
    ctx.strokeStyle = c.primary
    ctx.lineWidth = 2
    ctx.fillRect(x, y, cellW - 2, 50)
    ctx.strokeRect(x, y, cellW - 2, 50)
    ctx.fillStyle = c.text
    ctx.font = '14px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(v, x + (cellW - 2) / 2, y + 30)
    ctx.fillStyle = c.textSecondary
    ctx.font = '11px sans-serif'
    ctx.fillText(i, x + (cellW - 2) / 2, y + 70)
  })
}

const drawHash = (ctx) => {
  const w = 800, h = 220
  const c = colors.value
  const slots = 8
  const slotW = (w - 60) / slots
  data.forEach((v, i) => {
    const slot = v % slots
    const x = 30 + slot * slotW
    const y = 40 + (i % 3) * 46
    ctx.fillStyle = i === highlightIdx ? c.warning : c.primaryLight
    ctx.strokeStyle = c.primary
    ctx.lineWidth = 2
    ctx.fillRect(x, y, slotW - 10, 36)
    ctx.strokeRect(x, y, slotW - 10, 36)
    ctx.fillStyle = c.text
    ctx.font = '13px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(v, x + (slotW - 10) / 2, y + 23)
  })
  ctx.fillStyle = c.textSecondary
  ctx.font = '12px sans-serif'
  for (let s = 0; s < slots; s++) {
    ctx.fillText('slot ' + s, 30 + s * slotW + (slotW - 10) / 2, 18)
  }
}

const runSearch = async () => {
  const key = parseInt(target.value)
  if (isNaN(key)) { message.value = '请输入有效目标值'; return }
  if (running.value) return
  start()
  highlightIdx = -1
  rangeLow = -1; rangeHigh = -1
  message.value = ''
  draw()

  if (props.algoKey === 'sequential') await sequentialSearch(key)
  else if (props.algoKey === 'binary') await binarySearch(key)
  else if (props.algoKey === 'hash') await hashSearch(key)

  stop()
}

const runStepSearch = async () => {
  const key = parseInt(target.value)
  if (isNaN(key)) { message.value = '请输入有效目标值'; return }
  if (running.value) return
  startStepMode()
  highlightIdx = -1
  rangeLow = -1; rangeHigh = -1
  message.value = ''
  draw()

  if (props.algoKey === 'sequential') await sequentialSearch(key)
  else if (props.algoKey === 'binary') await binarySearch(key)
  else if (props.algoKey === 'hash') await hashSearch(key)

  stop()
}

const sequentialSearch = async (key) => {
  for (let i = 0; i < data.length; i++) {
    highlightIdx = i
    setDesc(`比较 arr[${i}]=${data[i]} 与目标 ${key}`)
    setLine(0)
    draw(); await sleep()
    if (data[i] === key) {
      setDesc(`命中！arr[${i}]=${key}`)
      message.value = `在索引 ${i} 处找到 ${key}`
      return
    }
  }
  setDesc(`扫描完毕，未找到 ${key}`)
  message.value = `未找到 ${key}`
}

const binarySearch = async (key) => {
  let low = 0, high = data.length - 1
  setLine(0)
  while (low <= high) {
    rangeLow = low; rangeHigh = high
    const mid = Math.floor((low + high) / 2)
    highlightIdx = mid
    setDesc(`查找区间 [${low}, ${high}]，取中点 arr[${mid}]=${data[mid]}`)
    setLine(2)
    draw(); await sleep()
    if (data[mid] === key) {
      setDesc(`命中！arr[${mid}]=${key}`)
      message.value = `在索引 ${mid} 处找到 ${key}`
      return
    } else if (data[mid] < key) {
      low = mid + 1
      setDesc(`arr[${mid}] < ${key}，目标在右半区 [${low}, ${high}]`)
      setLine(4)
    } else {
      high = mid - 1
      setDesc(`arr[${mid}] > ${key}，目标在左半区 [${low}, ${high}]`)
      setLine(5)
    }
  }
  setDesc('区间为空，未找到目标')
  message.value = `未找到 ${key}`
}

const hashSearch = async (key) => {
  const slots = 8
  const slot = key % slots
  setDesc(`计算散列地址：hash(${key}) = ${key} % ${slots} = ${slot}`)
  setLine(0)
  let probed = 0
  for (let i = 0; i < data.length; i++) {
    if (data[i] % slots === slot) {
      highlightIdx = i
      setDesc(`沿槽 ${slot} 探测第 ${++probed} 个元素 arr[${i}]=${data[i]}`)
      setLine(1)
      draw(); await sleep()
      if (data[i] === key) {
        setDesc(`命中！槽 ${slot} 中的 ${key}`)
        message.value = `在槽 ${slot} 中找到 ${key}`
        return
      }
    }
  }
  setDesc('探测完毕，未找到目标')
  message.value = `未找到 ${key}`
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    runStepSearch()
  }
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.viz-canvas { border: 1px solid var(--border); border-radius: 8px; display: block; max-width: 100%; }
.search-msg { font-size: 13px; color: var(--text-secondary); }
</style>