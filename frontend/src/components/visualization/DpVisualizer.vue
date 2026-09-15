<template>
  <div class="viz-root">
    <div class="viz-controls">
      <button class="btn btn-success btn-sm" @click="runDp">运行算法</button>
      <button class="btn btn-outline btn-sm" @click="resetDp">重置数据</button>
      <span v-if="message" class="dp-msg">{{ message }}</span>
    </div>

    <div class="viz-controls">
      <PlaybackControls
        :running="running.value"
        :paused="paused.value"
        :speed="speed.value"
        :step-mode="stepMode.value"
        :current-step="currentStep.value"
        @start="runDp"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="step"
        @reset="resetDp"
        @update:speed="v => speed.value = v"
      />
    </div>

    <div v-if="algoKey === 'knapsack'" class="dp-info">
      背包容量: <strong>{{ capacity }}</strong>
      <div class="items">
        <span v-for="(it, i) in items" :key="i" class="item-badge">
          物品{{ i+1 }}(w={{ it.w }}, v={{ it.v }})
        </span>
      </div>
    </div>

    <div v-if="algoKey === 'lcs'" class="dp-info">
      字符串 A: <strong>{{ strA }}</strong> &nbsp; 字符串 B: <strong>{{ strB }}</strong>
    </div>

    <div class="dp-table-wrap">
      <table class="dp-table">
        <tr v-for="(row, i) in table" :key="i">
          <td v-for="(cell, j) in row" :key="j" :class="{ active: activeCell && activeCell[0] === i && activeCell[1] === j }">
            {{ cell }}
          </td>
        </tr>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usePlayback } from '../../composables/usePlayback.js'
import PlaybackControls from './PlaybackControls.vue'

const props = defineProps({
  algoKey: { type: String, default: 'knapsack' }
})

const { running, paused, speed, stepMode, currentStep, start, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc } = usePlayback(50)

const capacity = ref(10)
const items = ref([])
const strA = ref('')
const strB = ref('')
const table = ref([])
const activeCell = ref(null)
const message = ref('')

onMounted(() => {
  resetDp()
})

const resetDp = () => {
  stop()
  resetPlayback()
  activeCell.value = null
  message.value = ''
  if (props.algoKey === 'knapsack') {
    capacity.value = 10
    items.value = [
      { w: 2, v: 3 }, { w: 3, v: 4 }, { w: 4, v: 5 }, { w: 5, v: 6 }
    ]
    const n = items.value.length
    table.value = Array.from({ length: n + 1 }, () => Array(capacity.value + 1).fill(0))
  } else {
    strA.value = 'ABCBDAB'
    strB.value = 'BDCABA'
    table.value = Array.from({ length: strA.value.length + 1 }, () => Array(strB.value.length + 1).fill(0))
  }
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const runDp = async () => {
  if (running.value) return
  start()
  message.value = ''
  if (props.algoKey === 'knapsack') await runKnapsack()
  else await runLcs()
  stop()
}

const runStepDp = async () => {
  if (running.value) return
  startStepMode()
  message.value = ''
  if (props.algoKey === 'knapsack') await runKnapsack()
  else await runLcs()
  stop()
}

const runKnapsack = async () => {
  const n = items.value.length
  const W = capacity.value
  const dp = Array.from({ length: n + 1 }, () => Array(W + 1).fill(0))
  for (let i = 1; i <= n && running.value; i++) {
    const it = items.value[i - 1]
    for (let w = 1; w <= W && running.value; w++) {
      dp[i][w] = dp[i - 1][w]
      if (w >= it.w) {
        dp[i][w] = Math.max(dp[i][w], dp[i - 1][w - it.w] + it.v)
      }
      activeCell.value = [i, w]
      table.value = dp.map(r => [...r])
      setDesc(`背包问题：考虑物品${i}(w=${it.w},v=${it.v})，容量 ${w}：dp[${i}][${w}] = ${dp[i][w]}${
        w >= it.w ? `（选入则 +${it.v}）` : '（装不下，仅忽略）'
      }`)
      await sleep()
    }
  }
  activeCell.value = null
  setDesc('填表完成')
  message.value = `最大价值: ${dp[n][W]}`
}

const runLcs = async () => {
  const A = strA.value, B = strB.value
  const m = A.length, n = B.length
  const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0))
  for (let i = 1; i <= m && running.value; i++) {
    for (let j = 1; j <= n && running.value; j++) {
      if (A[i - 1] === B[j - 1]) {
        dp[i][j] = dp[i - 1][j - 1] + 1
        setDesc(`LCS：A[${i - 1}]=${A[i - 1]} == B[${j - 1}]=${B[j - 1]}，dp[${i}][${j}] = dp[${i - 1}][${j - 1}]+1 = ${dp[i][j]}`)
      } else {
        dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1])
        setDesc(`LCS：字符不同，取上方/左方较大值 dp[${i}][${j}] = ${dp[i][j]}`)
      }
      activeCell.value = [i, j]
      table.value = dp.map(r => [...r])
      await sleep()
    }
  }
  activeCell.value = null
  setDesc('填表完成')
  message.value = `最长公共子序列长度: ${dp[m][n]}`
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    runStepDp()
  }
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.dp-info { font-size: 14px; }
.items { display: flex; gap: 8px; margin-top: 6px; flex-wrap: wrap; }
.item-badge { padding: 4px 10px; background: var(--primary-light); border: 1px solid var(--primary); border-radius: 6px; font-size: 12px; color: var(--text); }
.dp-table-wrap { overflow-x: auto; }
.dp-table { border-collapse: collapse; font-size: 13px; }
.dp-table td { border: 1px solid var(--border); padding: 8px 12px; text-align: center; min-width: 32px; background: var(--bg-card); color: var(--text); }
.dp-table td.active { background: var(--warning); color: #fff; font-weight: 600; }
.dp-msg { font-size: 13px; color: var(--text-secondary); }
</style>