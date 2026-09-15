<template>
  <div class="viz-root">
    <div class="viz-controls">
      <label class="form-label" style="margin:0">主串 t</label>
      <input class="form-input" v-model="textValue" style="width:200px" />
      <label class="form-label" style="margin:0">模式串 p</label>
      <input class="form-input" v-model="patternValue" style="width:140px" />
      <button class="btn btn-success btn-sm" @click="runKmp">运行 KMP</button>
      <button class="btn btn-outline btn-sm" @click="resetKmp">重置</button>
      <span v-if="resultMsg" class="kmp-msg">{{ resultMsg }}</span>
    </div>

    <div class="viz-controls">
      <PlaybackControls
        :running="running.value"
        :paused="paused.value"
        :speed="speed.value"
        :step-mode="stepMode.value"
        :current-step="currentStep.value"
        @start="runKmp"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="step"
        @reset="resetKmp"
        @update:speed="v => speed.value = v"
      />
    </div>

    <canvas ref="kmpCanvas" width="800" height="260" class="viz-canvas"></canvas>

    <div class="kmp-next">
      <span class="next-label">next[]:</span>
      <span v-for="(v, i) in nextArr" :key="i" class="next-item" :class="{ cur: nextFocus === i }">{{ v }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useVisualizationTheme } from '../../composables/useVisualizationTheme.js'
import { usePlayback } from '../../composables/usePlayback.js'
import { useCrispCanvas } from '../../composables/useCrispCanvas.js'
import PlaybackControls from './PlaybackControls.vue'

const { colors } = useVisualizationTheme()
const { running, paused, speed, stepMode, currentStep, start, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc, setLine } = usePlayback(120)
const { crisp } = useCrispCanvas()

const kmpCanvas = ref(null)
const textValue = ref('ABABCABAB')
const patternValue = ref('ABAB')
const nextArr = ref([])
const nextFocus = ref(-1)
const resultMsg = ref('')

let textIdx = -1
let patIdx = -1
let matchPos = -1
let built = false

const CELL_W = 38
const CELL_H = 44
const START_X = 70

onMounted(() => {
  resetKmp()
  crisp(kmpCanvas.value, 800, 260)
})

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const resetKmp = () => {
  stop()
  resetPlayback()
  textIdx = -1; patIdx = -1; matchPos = -1; built = false
  nextArr.value = []
  nextFocus.value = -1
  resultMsg.value = ''
  draw()
}

const runKmp = async () => {
  if (running.value) return
  const t = textValue.value, p = patternValue.value
  if (!t || !p) { resultMsg.value = '请输入合法的主串与模式串'; return }
  start()
  textIdx = -1; patIdx = -1; matchPos = -1; built = false
  nextArr.value = []; nextFocus.value = -1
  resultMsg.value = ''
  draw()

  await animateBuildNext(p)
  if (!running.value) { stop(); return }

  const matches = await animateMatch(t, p, nextArr.value)
  if (matches.length === 0) resultMsg.value = `未在主串中找到 "${p}"`
  else resultMsg.value = `匹配成功：共 ${matches.length} 处，起始位置 ${matches.join(', ')}`
  setDesc('KMP 匹配完成')
  stop()
}

const runStepKmp = async () => {
  if (running.value) return
  const t = textValue.value, p = patternValue.value
  if (!t || !p) { resultMsg.value = '请输入合法的主串与模式串'; return }
  startStepMode()
  textIdx = -1; patIdx = -1; matchPos = -1; built = false
  nextArr.value = []; nextFocus.value = -1
  resultMsg.value = ''
  draw()

  await animateBuildNext(p)
  if (!running.value) { stop(); return }

  const matches = await animateMatch(t, p, nextArr.value)
  if (matches.length === 0) resultMsg.value = `未在主串中找到 "${p}"`
  else resultMsg.value = `匹配成功：共 ${matches.length} 处，起始位置 ${matches.join(', ')}`
  setDesc('KMP 匹配完成')
  stop()
}

const handleSwitchToStepMode = () => {
  if (running.value) switchToStepMode()
  else runStepKmp()
}

const animateBuildNext = async (p) => {
  const m = p.length
  const next = new Array(m).fill(0)
  built = true
  nextArr.value = [...next]
  nextFocus.value = 0
  setDesc('构造前缀函数 next[]：next[0] = 0')
  setLine(1)
  draw(); await sleep()

  let j = 0
  for (let i = 1; i < m && running.value; i++) {
    patIdx = j; nextFocus.value = i
    setDesc(`计算 next[${i}]：从 j=${j} 开始尝试最长相等前后缀`)
    draw(); if (!await sleep()) return
    while (j > 0 && p[i] !== p[j] && running.value) {
      setDesc(`p[${i}]=${p[i]} ≠ p[${j}]=${p[j]}，j 回退到 next[${j - 1}]=${next[j - 1]}`)
      setLine(3)
      draw(); if (!await sleep()) return
      j = next[j - 1]
      patIdx = j
    }
    if (p[i] === p[j]) {
      setDesc(`p[${i}]=${p[i]} == p[${j}]=${p[j]}，最长相等前后缀 +1`)
      setLine(4)
      j++
    } else {
      setDesc(`p[${i}]=${p[i]} 与 p[0]=${p[0]} 不相等，next[${i}]=0`)
    }
    next[i] = j
    nextArr.value = [...next]
    patIdx = nextFocus.value = -1
    setLine(4)
    draw(); if (!await sleep()) return
  }
  setDesc('next[] 构造完成：' + next.join(', '))
  nextFocus.value = -1
  patIdx = -1
  draw(); await sleep()
}

const animateMatch = async (t, p, next) => {
  const n = t.length, m = p.length
  const matches = []
  let i = 0, j = 0
  while (i < n && running.value) {
    textIdx = i; patIdx = j
    if (t[i] === p[j]) {
      setDesc(`匹配：t[${i}]=${t[i]} == p[${j}]=${p[j]}，i、j 同时后移`)
      setLine(4)
      draw(); if (!await sleep()) return matches
      i++; j++
      if (j === m) {
        matches.push(i - m)
        setDesc(`匹配成功！模式串在主串起始位置 ${i - m}`)
        setLine(6)
        matchPos = i - m
        draw(); if (!await sleep()) return matches
        setDesc(`已记录匹配，j 由 ${j} 回退到 next[${j - 1}]=${next[j - 1]}，继续寻找下一处`)
        j = next[j - 1]
        matchPos = -1
      }
    } else if (j > 0) {
      setDesc(`失配：t[${i}]=${t[i]} ≠ p[${j}]=${p[j]}，i 不动，j 回溯到 next[${j - 1}]=${next[j - 1]}`)
      setLine(5)
      draw(); if (!await sleep()) return matches
      j = next[j - 1]
    } else {
      setDesc(`失配且 j=0：t[${i}]=${t[i]} ≠ p[0]=${p[0]}，主串指针 i 后移（模式串不动）`)
      setLine(5)
      draw(); if (!await sleep()) return matches
      i++
    }
  }
  textIdx = patIdx = -1
  return matches
}

const draw = () => {
  const canvas = kmpCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 800
  const c = colors.value
  ctx.fillStyle = c.bg
  ctx.fillRect(0, 0, w, 260)

  const drawRow = (cells, y, label, labelColor, highlightIdx, hiColor, allMatched) => {
    ctx.fillStyle = labelColor
    ctx.font = '13px sans-serif'
    ctx.textAlign = 'left'
    ctx.fillText(label, 8, y + CELL_H / 2 + 4)
    for (let k = 0; k < cells.length; k++) {
      const x = START_X + k * CELL_W
      let fill = c.primaryLight
      let stroke = c.primary
      if (k === highlightIdx) { fill = hiColor; stroke = hiColor }
      else if (allMatched && allMatched(k)) { fill = c.successLight; stroke = c.success }
      ctx.fillStyle = fill
      ctx.strokeStyle = stroke
      ctx.lineWidth = 2
      ctx.fillRect(x, y, CELL_W - 3, CELL_H)
      ctx.strokeRect(x, y, CELL_W - 3, CELL_H)
      ctx.fillStyle = c.text
      ctx.font = '16px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(cells[k], x + (CELL_W - 3) / 2, y + CELL_H / 2 + 6)
    }
  }

  const t = textValue.value, p = patternValue.value
  drawRow(t, 30, '主串 t', c.textSecondary, textIdx, c.warning, (k) => matchPos >= 0 && k >= matchPos && k < matchPos + p.length)
  drawRow(p, 106, '模式串 p', c.textSecondary, patIdx, built ? c.accent : c.warning, (k) => false)

  if (built && nextArr.value.length) {
    ctx.fillStyle = c.textSecondary
    ctx.font = '13px sans-serif'
    ctx.textAlign = 'left'
    ctx.fillText('next[]', 8, 190 + 15)
    for (let k = 0; k < nextArr.value.length; k++) {
      const x = START_X + k * CELL_W
      ctx.fillStyle = k === nextFocus ? c.warning : c.primaryLight
      ctx.strokeStyle = k === nextFocus ? c.warning : c.primary
      ctx.lineWidth = 2
      ctx.fillRect(x, 186, 24, 24)
      ctx.strokeRect(x, 186, 24, 24)
      ctx.fillStyle = c.text
      ctx.font = '13px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(nextArr.value[k], x + 12, 203)
    }
  }
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.viz-canvas { border: 1px solid var(--border); border-radius: 8px; display: block; max-width: 100%; }
.kmp-msg { font-size: 13px; color: var(--text-secondary); }
.kmp-next { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--text); flex-wrap: wrap; }
.next-label { color: var(--text-secondary); margin-right: 4px; }
.next-item { min-width: 26px; height: 26px; line-height: 26px; text-align: center; border-radius: 6px; background: var(--primary-light); border: 1px solid var(--primary); color: var(--text); }
.next-item.cur { background: var(--warning); color: #fff; font-weight: 700; }
</style>