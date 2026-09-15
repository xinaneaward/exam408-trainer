<template>
  <div class="viz-root">
    <div class="viz-controls">
      <input class="form-input" v-model.number="inputVal" placeholder="值" style="width:80px" type="number" />
      <button class="btn btn-primary btn-sm" @click="insertHead">头插</button>
      <button class="btn btn-primary btn-sm" @click="insertTail">尾插</button>
      <button class="btn btn-danger btn-sm" @click="deleteValue">删除</button>
      <button class="btn btn-outline btn-sm" @click="reverseList">反转</button>
      <button class="btn btn-outline btn-sm" @click="resetList">重置</button>
      <span v-if="message" class="list-msg">{{ message }}</span>
    </div>

    <div class="viz-controls">
      <PlaybackControls
        :running="running.value"
        :paused="paused.value"
        :speed="speed.value"
        :step-mode="stepMode.value"
        :current-step="currentStep.value"
        @start="reverseList"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="step"
        @reset="resetList"
        @update:speed="v => speed.value = v"
      />
    </div>

    <canvas ref="listCanvas" width="800" height="220" class="viz-canvas"></canvas>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useVisualizationTheme } from '../../composables/useVisualizationTheme.js'
import { usePlayback } from '../../composables/usePlayback.js'
import { useCrispCanvas } from '../../composables/useCrispCanvas.js'
import PlaybackControls from './PlaybackControls.vue'

const { colors } = useVisualizationTheme()
const { running, paused, speed, stepMode, currentStep, start, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc, setLine } = usePlayback(100)
const { crisp } = useCrispCanvas()

const listCanvas = ref(null)
const inputVal = ref('')
const message = ref('')

class ListNode {
  constructor(val) { this.val = val; this.next = null }
}

let head = null
let highlightVal = null

onMounted(() => {
  resetList()
  crisp(listCanvas.value, 800, 220)
})

const resetList = () => {
  stop()
  resetPlayback()
  head = new ListNode(10)
  head.next = new ListNode(25)
  head.next.next = new ListNode(40)
  head.next.next.next = new ListNode(55)
  message.value = ''
  highlightVal = null
  drawList()
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const drawList = () => {
  const canvas = listCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 800, h = 220
  const c = colors.value
  ctx.fillStyle = c.bg
  ctx.fillRect(0, 0, w, h)

  let node = head
  let x = 40
  const y = h / 2
  let idx = 0
  while (node && x < w - 90) {
    const isHigh = highlightVal !== null && node.val === highlightVal
    ctx.fillStyle = isHigh ? c.warning : c.primaryLight
    ctx.strokeStyle = c.primary
    ctx.lineWidth = 2
    ctx.fillRect(x, y - 24, 50, 48)
    ctx.strokeRect(x, y - 24, 50, 48)
    ctx.fillStyle = c.text
    ctx.font = '14px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(node.val, x + 25, y + 4)

    if (node.next) {
      ctx.beginPath()
      ctx.moveTo(x + 50, y)
      ctx.lineTo(x + 82, y)
      ctx.strokeStyle = c.edge
      ctx.lineWidth = 2
      ctx.stroke()
      ctx.beginPath()
      ctx.moveTo(x + 76, y - 5)
      ctx.lineTo(x + 82, y)
      ctx.lineTo(x + 76, y + 5)
      ctx.stroke()
    }

    x += 92
    node = node.next
    idx++
  }
  if (!head) {
    ctx.fillStyle = c.textSecondary
    ctx.font = '14px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText('空链表', w / 2, y)
  }
}

const insertHead = async () => {
  const val = parseInt(inputVal.value)
  if (isNaN(val)) { message.value = '请输入有效数字'; return }
  const node = new ListNode(val)
  node.next = head
  head = node
  highlightVal = val
  inputVal.value = ''
  message.value = `在头部插入 ${val}`
  setDesc(`头插：新结点 ${val} 指向原头结点，成为新表头`)
  drawList(); await sleep()
  highlightVal = null; drawList()
}

const insertTail = async () => {
  const val = parseInt(inputVal.value)
  if (isNaN(val)) { message.value = '请输入有效数字'; return }
  const node = new ListNode(val)
  if (!head) { head = node }
  else {
    let cur = head
    while (cur.next) cur = cur.next
    cur.next = node
  }
  highlightVal = val
  inputVal.value = ''
  message.value = `在尾部插入 ${val}`
  setDesc(`尾插：沿 next 指针遍历找到表尾，接入新结点 ${val}`)
  drawList(); await sleep()
  highlightVal = null; drawList()
}

const deleteValue = async () => {
  const val = parseInt(inputVal.value)
  if (isNaN(val)) { message.value = '请输入有效数字'; return }
  if (!head) { message.value = '链表为空'; return }
  if (head.val === val) {
    head = head.next
    message.value = `删除 ${val}`
    inputVal.value = ''
    drawList(); return
  }
  let cur = head
  while (cur.next && cur.next.val !== val) cur = cur.next
  if (cur.next) {
    cur.next = cur.next.next
    message.value = `删除 ${val}`
  } else {
    message.value = `未找到 ${val}`
  }
  inputVal.value = ''
  drawList()
}

const reverseList = async () => {
  start()
  let prev = null, cur = head
  let count = 0
  while (cur) {
    const val = cur.val
    const next = cur.next
    cur.next = prev
    prev = cur
    cur = next
    count++
    setDesc(`反转第 ${count} 个结点 ${val}：指针指向前驱${count === 1 ? '（NULL，成为新表头）' : '（上一次处理的结点）'}`)
    setLine(1)
    drawList(); await sleep()
  }
  head = prev
  message.value = '链表已反转'
  setDesc('链表反转完成')
  drawList()
  stop()
}

const reverseListStep = async () => {
  startStepMode()
  let prev = null, cur = head
  let count = 0
  while (cur) {
    const val = cur.val
    const next = cur.next
    cur.next = prev
    prev = cur
    cur = next
    count++
    setDesc(`反转第 ${count} 个结点 ${val}：指针指向前驱${count === 1 ? '（NULL，成为新表头）' : '（上一次处理的结点）'}`)
    setLine(1)
    drawList(); await sleep()
  }
  head = prev
  message.value = '链表已反转'
  setDesc('链表反转完成')
  drawList()
  stop()
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    reverseListStep()
  }
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.viz-canvas { border: 1px solid var(--border); border-radius: 8px; display: block; max-width: 100%; }
.list-msg { font-size: 13px; color: var(--text-secondary); }
</style>