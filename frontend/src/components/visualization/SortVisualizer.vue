<template>
  <div class="viz-root">
    <div class="viz-controls">
      <button class="btn btn-primary btn-sm" @click="generateArray">生成随机数组</button>
      <label class="form-label" style="margin:0">数组大小</label>
      <input class="form-input" type="number" v-model.number="arraySize" style="width:70px" min="5" max="80" />
      <PlaybackControls
        :running="running"
        :paused="paused"
        :speed="speed"
        :step-mode="stepMode"
        :current-step="currentStep"
        @start="startSort"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="handleStep"
        @reset="resetSort"
        @update:speed="v => speed.value = v"
      />
    </div>

    <canvas ref="sortCanvas" width="800" height="300" class="viz-canvas"></canvas>

    <div class="viz-stats">
      <span class="stat-card">比较次数: <strong>{{ comparisons }}</strong></span>
      <span class="stat-card">交换次数: <strong>{{ swaps }}</strong></span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useVisualizationTheme } from '../../composables/useVisualizationTheme.js'
import { usePlayback } from '../../composables/usePlayback.js'
import { useCrispCanvas } from '../../composables/useCrispCanvas.js'
import PlaybackControls from './PlaybackControls.vue'

const props = defineProps({
  algoKey: { type: String, default: 'bubble' }
})

const { colors } = useVisualizationTheme()
const { running, paused, speed, stepMode, currentStep, start, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc, setLine } = usePlayback(50)
const { crisp } = useCrispCanvas()

const sortCanvas = ref(null)
const arraySize = ref(24)
const comparisons = ref(0)
const swaps = ref(0)

let sortArr = []
let sortColors = []

onMounted(() => {
  generateArray()
  crisp(sortCanvas.value, 800, 300)
})

watch(() => props.algoKey, () => {
  resetSort()
})

const generateArray = () => {
  if (running.value) return
  const size = arraySize.value || 24
  sortArr = Array.from({ length: size }, () => Math.floor(Math.random() * 240) + 20)
  sortColors = new Array(size).fill('default')
  comparisons.value = 0
  swaps.value = 0
  drawSort()
}

const resetSort = () => {
  stop()
  resetPlayback()
  generateArray()
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const drawSort = () => {
  const canvas = sortCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 800, h = 300
  const c = colors.value
  ctx.fillStyle = c.bg
  ctx.fillRect(0, 0, w, h)
  ctx.strokeStyle = c.border
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(0, h - 10)
  ctx.lineTo(w, h - 10)
  ctx.stroke()
  const barW = Math.max(3, (w - 20) / sortArr.length - 2)
  const gap = barW + 2
  sortArr.forEach((v, i) => {
    const x = 10 + i * gap
    const barH = (v / 280) * (h - 30)
    let fill = c.primary
    const sc = sortColors[i]
    if (sc === 'compare') fill = c.warning
    else if (sc === 'swap') fill = c.danger
    else if (sc === 'sorted') fill = c.success
    else if (sc === 'pivot') fill = c.accent
    ctx.fillStyle = fill
    ctx.fillRect(x, h - 10 - barH, barW, barH)
    if (barW > 12) {
      ctx.fillStyle = c.text
      ctx.font = '10px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(v, x + barW / 2, h - 14 - barH)
    }
  })
}

const mark = (idx, type) => {
  if (idx >= 0 && idx < sortColors.length) sortColors[idx] = type
}
const clearMarks = (type) => {
  sortColors = sortColors.map(c => c === type ? 'default' : c)
}
const allDefault = () => {
  sortColors = new Array(sortArr.length).fill('default')
}

const startSort = async () => {
  if (running.value) return
  start()
  comparisons.value = 0
  swaps.value = 0
  allDefault()
  drawSort()
  await sleep(speed.value)
  try {
    switch (props.algoKey) {
      case 'bubble': await bubbleSort(); break
      case 'quick': await quickSort(0, sortArr.length - 1); break
      case 'heap': await heapSort(); break
      case 'merge': await mergeSort(0, sortArr.length - 1); break
      case 'insertion': await insertionSort(); break
      case 'selection': await selectionSort(); break
      case 'shell': await shellSort(); break
    }
    sortColors = new Array(sortArr.length).fill('sorted')
    drawSort()
  } catch (e) { /* aborted */ }
  stop()
}

const startStepSort = async () => {
  if (running.value) return
  startStepMode()
  comparisons.value = 0
  swaps.value = 0
  allDefault()
  drawSort()
  try {
    switch (props.algoKey) {
      case 'bubble': await bubbleSort(); break
      case 'quick': await quickSort(0, sortArr.length - 1); break
      case 'heap': await heapSort(); break
      case 'merge': await mergeSort(0, sortArr.length - 1); break
      case 'insertion': await insertionSort(); break
      case 'selection': await selectionSort(); break
      case 'shell': await shellSort(); break
    }
    sortColors = new Array(sortArr.length).fill('sorted')
    drawSort()
  } catch (e) { /* aborted */ }
  stop()
}

const handleStep = () => {
  step()
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    startStepSort()
  }
}

const bubbleSort = async () => {
  const n = sortArr.length
  for (let i = 0; i < n - 1 && running.value; i++) {
    setLine(0)
    for (let j = 0; j < n - 1 - i && running.value; j++) {
      mark(j, 'compare'); mark(j + 1, 'compare')
      setDesc(`比较 arr[${j}]=${sortArr[j]} 与 arr[${j + 1}]=${sortArr[j + 1]}`)
      setLine(2)
      drawSort(); if (!await sleep()) return
      comparisons.value++
      if (sortArr[j] > sortArr[j + 1]) {
        mark(j, 'swap'); mark(j + 1, 'swap')
        setDesc(`arr[${j}] > arr[${j + 1}]，交换两个元素`)
        setLine(3)
        drawSort(); if (!await sleep()) return
        ;[sortArr[j], sortArr[j + 1]] = [sortArr[j + 1], sortArr[j]]
        swaps.value++
      }
      mark(j, 'default'); mark(j + 1, 'default')
    }
    mark(n - 1 - i, 'sorted')
  }
  if (running.value) mark(0, 'sorted')
  setDesc('排序完成')
}

const selectionSort = async () => {
  const n = sortArr.length
  for (let i = 0; i < n - 1 && running.value; i++) {
    let minIdx = i
    mark(i, 'pivot')
    setDesc(`第 ${i + 1} 趟：假设 arr[${i}]=${sortArr[i]} 为最小，从 ${i + 1} 往后找更小值`)
    setLine(0)
    drawSort(); if (!await sleep()) return
    for (let j = i + 1; j < n && running.value; j++) {
      mark(j, 'compare')
      setDesc(`比较 arr[${j}]=${sortArr[j]} 与当前最小 arr[${minIdx}]=${sortArr[minIdx]}`)
      setLine(2)
      drawSort(); if (!await sleep()) return
      comparisons.value++
      if (sortArr[j] < sortArr[minIdx]) {
        if (minIdx !== i) mark(minIdx, 'default')
        minIdx = j
        mark(minIdx, 'pivot')
        setDesc(`arr[${j}]=${sortArr[j]} 更小，更新最小下标为 ${j}`)
        drawSort(); if (!await sleep()) return
      } else {
        mark(j, 'default')
      }
    }
    if (minIdx !== i && running.value) {
      mark(i, 'swap'); mark(minIdx, 'swap')
      setDesc(`将最小值 arr[${minIdx}]=${sortArr[minIdx]} 与 arr[${i}]=${sortArr[i]} 交换`)
      setLine(4)
      drawSort(); if (!await sleep()) return
      ;[sortArr[i], sortArr[minIdx]] = [sortArr[minIdx], sortArr[i]]
      swaps.value++
    }
    mark(minIdx, 'default')
    mark(i, 'sorted')
    drawSort()
  }
  mark(n - 1, 'sorted')
  setDesc('排序完成')
}

const insertionSort = async () => {
  const n = sortArr.length
  mark(0, 'sorted')
  for (let i = 1; i < n && running.value; i++) {
    const key = sortArr[i]
    let j = i - 1
    mark(i, 'compare')
    setDesc(`取出 arr[${i}]=${key}，向前寻找插入位置`)
    setLine(0)
    drawSort(); if (!await sleep()) return
    while (j >= 0 && running.value) {
      mark(j, 'compare')
      setDesc(`比较 arr[${j}]=${sortArr[j]} 与 key=${key}`)
      setLine(2)
      drawSort(); if (!await sleep()) return
      comparisons.value++
      if (sortArr[j] > key) {
        mark(j + 1, 'swap'); mark(j, 'swap')
        setDesc(`arr[${j}] > key，将 arr[${j + 1}] 后移为 ${sortArr[j]}`)
        setLine(3)
        drawSort(); if (!await sleep()) return
        sortArr[j + 1] = sortArr[j]
        swaps.value++
        mark(j + 1, 'sorted')
        mark(j, 'default')
        j--
      } else {
        mark(j, 'default')
        setDesc(`arr[${j}] <= key，找到插入位置 ${j + 1}`)
        break
      }
    }
    sortArr[j + 1] = key
    mark(i, 'default')
    mark(j + 1, 'sorted')
    setDesc(`将 key=${key} 插入位置 ${j + 1}`)
    setLine(5)
    drawSort(); if (!await sleep()) return
  }
  setDesc('排序完成')
}

const shellSort = async () => {
  const n = sortArr.length
  for (let gap = Math.floor(n / 2); gap > 0 && running.value; gap = Math.floor(gap / 2)) {
    setDesc(`增量 gap=${gap}，按该增量分组进行插入排序`)
    setLine(0)
    for (let i = gap; i < n && running.value; i++) {
      const key = sortArr[i]
      let j = i
      mark(i, 'compare')
      setDesc(`组内取 arr[${i}]=${key}，步长为 ${gap} 向前比较`)
      drawSort(); if (!await sleep()) return
      while (j >= gap && running.value) {
        comparisons.value++
        mark(j - gap, 'compare')
        setDesc(`比较 arr[${j - gap}]=${sortArr[j - gap]} 与 arr[${j}]=${sortArr[j]}`)
        setLine(4)
        drawSort(); if (!await sleep()) return
        if (sortArr[j - gap] > key) {
          mark(j, 'swap'); mark(j - gap, 'swap')
          setDesc(`arr[${j - gap}] > key，后移 arr[${j}]=${sortArr[j - gap]}`)
          setLine(3)
          drawSort(); if (!await sleep()) return
          sortArr[j] = sortArr[j - gap]
          swaps.value++
          mark(j, 'default')
          mark(j - gap, 'default')
          j -= gap
        } else {
          mark(j - gap, 'default')
          setDesc(`arr[${j - gap}] <= key，插入位置 ${j}`)
          break
        }
      }
      sortArr[j] = key
      mark(i, 'default')
      mark(j, 'sorted')
      drawSort(); if (!await sleep()) return
    }
  }
  allDefault()
  for (let i = 0; i < n; i++) mark(i, 'sorted')
  setDesc('排序完成')
}

const quickSort = async (low, high) => {
  if (low >= high || !running.value) return
  const pivotIdx = await partition(low, high)
  if (!running.value) return
  mark(pivotIdx, 'sorted'); drawSort()
  await quickSort(low, pivotIdx - 1)
  await quickSort(pivotIdx + 1, high)
}

const partition = async (low, high) => {
  const pivot = sortArr[high]
  mark(high, 'pivot')
  setDesc(`以 arr[${high}]=${pivot} 为枢轴，划分区间 [${low}, ${high}]`)
  setLine(0)
  drawSort(); if (!await sleep()) return high
  let i = low - 1
  for (let j = low; j < high && running.value; j++) {
    mark(j, 'compare')
    setDesc(`比较 arr[${j}]=${sortArr[j]} 与枢轴 ${pivot}`)
    setLine(1)
    drawSort(); if (!await sleep()) return high
    comparisons.value++
    if (sortArr[j] <= pivot) {
      i++
      if (i !== j) {
        mark(i, 'swap'); mark(j, 'swap')
        setDesc(`arr[${j}] <= 枢轴，与 arr[${i}] 交换`)
        setLine(5)
        drawSort(); if (!await sleep()) return high
        ;[sortArr[i], sortArr[j]] = [sortArr[j], sortArr[i]]
        swaps.value++
      }
    }
    mark(j, 'default')
    if (i >= 0) mark(i, 'default')
  }
  mark(high, 'default'); i++
  if (i !== high && running.value) {
    mark(i, 'swap'); mark(high, 'swap')
    setDesc(`枢轴 ${pivot} 就位，与 arr[${i}] 交换`)
    drawSort(); if (!await sleep()) return high
    ;[sortArr[i], sortArr[high]] = [sortArr[high], sortArr[i]]
    swaps.value++
  }
  mark(high, 'default')
  setDesc(`枢轴 ${pivot} 到达最终位置 ${i}，左边 <= 枢轴，右边 > 枢轴`)
  return i
}

const heapSort = async () => {
  const n = sortArr.length
  for (let i = Math.floor(n / 2) - 1; i >= 0 && running.value; i--) {
    setDesc(`从最后一个非叶结点 arr[${i}] 开始自下而上调整建立大顶堆`)
    setLine(0)
    await heapify(n, i)
  }
  for (let i = n - 1; i > 0 && running.value; i--) {
    mark(0, 'swap'); mark(i, 'swap')
    setDesc(`将堆顶最大元素 ${sortArr[0]} 与末尾 arr[${i}] 交换`)
    setLine(1)
    drawSort(); if (!await sleep()) return
    ;[sortArr[0], sortArr[i]] = [sortArr[i], sortArr[0]]
    swaps.value++
    mark(0, 'default'); mark(i, 'sorted')
    drawSort()
    await heapify(i, 0)
  }
  if (running.value) mark(0, 'sorted')
  setDesc('排序完成')
}

const heapify = async (size, root) => {
  let largest = root
  const left = 2 * root + 1, right = 2 * root + 2
  const c = colors.value
  if (left < size) {
    comparisons.value++
    mark(left, 'compare'); mark(root, 'compare')
    setDesc(`比较结点 arr[${root}]=${sortArr[root]} 与左孩子 arr[${left}]=${sortArr[left]}`)
    setLine(2)
    drawSort(); if (!await sleep()) return
    if (sortArr[left] > sortArr[largest]) largest = left
    mark(left, 'default'); mark(root, 'default')
  }
  if (right < size) {
    comparisons.value++
    mark(right, 'compare'); mark(root, 'compare')
    setDesc(`再比较根与右孩子 arr[${right}]=${sortArr[right]}`)
    drawSort(); if (!await sleep()) return
    if (sortArr[right] > sortArr[largest]) largest = right
    mark(right, 'default'); mark(root, 'default')
  }
  if (largest !== root && running.value) {
    mark(root, 'swap'); mark(largest, 'swap')
    setDesc(`最大元素 ${sortArr[largest]}（arr[${largest}]）与根 arr[${root}] 交换`)
    setLine(3)
    drawSort(); if (!await sleep()) return
    ;[sortArr[root], sortArr[largest]] = [sortArr[largest], sortArr[root]]
    swaps.value++
    mark(root, 'default'); mark(largest, 'default')
    await heapify(size, largest)
  }
}

const mergeSort = async (left, right) => {
  if (left >= right || !running.value) return
  const mid = Math.floor((left + right) / 2)
  setDesc(`分割区间 [${left}, ${right}]，取中点 mid=${mid}`)
  setLine(0)
  await mergeSort(left, mid)
  await mergeSort(mid + 1, right)
  await merge(left, mid, right)
}

const merge = async (left, mid, right) => {
  const temp = []
  let i = left, j = mid + 1
  for (let k = left; k <= right; k++) mark(k, 'compare')
  setDesc(`合并区间 [${left}, ${right}]：左右两半均为有序`)
  setLine(2)
  drawSort(); if (!await sleep()) return
  while (i <= mid && j <= right && running.value) {
    comparisons.value++
    setDesc(`比较 arr[${i}]=${sortArr[i]} 与 arr[${j}]=${sortArr[j]}，取较小者`)
    drawSort(); if (!await sleep()) return
    if (sortArr[i] <= sortArr[j]) temp.push(sortArr[i++])
    else temp.push(sortArr[j++])
  }
  while (i <= mid) temp.push(sortArr[i++])
  while (j <= right) temp.push(sortArr[j++])
  for (let k = 0; k < temp.length && running.value; k++) {
    sortArr[left + k] = temp[k]
    mark(left + k, 'swap')
    setDesc(`将 ${temp[k]} 写回位置 ${left + k}`)
    drawSort(); if (!await sleep()) return
    swaps.value++
    mark(left + k, 'default')
  }
  setDesc(`区间 [${left}, ${right}] 合并完成`)
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.viz-canvas { border: 1px solid var(--border); border-radius: 8px; display: block; max-width: 100%; }
.viz-stats { display: flex; gap: 12px; font-size: 13px; }
</style>