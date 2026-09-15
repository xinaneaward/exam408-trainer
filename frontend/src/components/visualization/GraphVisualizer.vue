<template>
  <div class="viz-root">
    <div class="viz-controls">
      <button v-if="isTraversal" class="btn btn-outline btn-sm" @click="runGraph('dfs')">DFS</button>
      <button v-if="isTraversal" class="btn btn-outline btn-sm" @click="runGraph('bfs')">BFS</button>
      <button v-if="algoKey === 'dijkstra'" class="btn btn-success btn-sm" @click="runDijkstra">运行 Dijkstra</button>
      <button v-if="algoKey === 'prim'" class="btn btn-success btn-sm" @click="runPrim">运行 Prim</button>
      <button v-if="algoKey === 'kruskal'" class="btn btn-success btn-sm" @click="runKruskal">运行 Kruskal</button>
      <button v-if="algoKey === 'topological'" class="btn btn-success btn-sm" @click="runTopological">运行拓扑排序</button>
      <button v-if="algoKey === 'floyd'" class="btn btn-success btn-sm" @click="runFloyd">运行 Floyd</button>
      <button class="btn btn-outline btn-sm" @click="resetGraph">重置</button>
      <span v-if="message" class="graph-msg">{{ message }}</span>
    </div>

    <div class="viz-controls">
      <PlaybackControls
        :running="running.value"
        :paused="paused.value"
        :speed="speed.value"
        :step-mode="stepMode.value"
        :current-step="currentStep.value"
        @start="startCurrent"
        @switchtostepmode="handleSwitchToStepMode"
        @pause="togglePause"
        @step="step"
        @reset="resetGraph"
        @update:speed="v => speed.value = v"
      />
    </div>

    <canvas ref="graphCanvas" width="800" height="420" class="viz-canvas"></canvas>

    <div v-if="floydResult" class="floyd-result">
      <table>
        <tr v-for="(row, i) in floydResult" :key="i">
          <td v-for="(v, j) in row" :key="j">{{ v }}</td>
        </tr>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useVisualizationTheme } from '../../composables/useVisualizationTheme.js'
import { usePlayback } from '../../composables/usePlayback.js'
import { useCrispCanvas } from '../../composables/useCrispCanvas.js'
import PlaybackControls from './PlaybackControls.vue'

const props = defineProps({
  algoKey: { type: String, default: 'dfs' }
})

const { colors } = useVisualizationTheme()
const { running, paused, speed, stepMode, currentStep, start: playbackStart, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc, setLine } = usePlayback(100)
const { crisp } = useCrispCanvas()

const graphCanvas = ref(null)
const message = ref('')
const floydResult = ref(null)

const graphNodes = [
  { id: 'A', x: 400, y: 60 },
  { id: 'B', x: 220, y: 160 },
  { id: 'C', x: 580, y: 160 },
  { id: 'D', x: 120, y: 320 },
  { id: 'E', x: 400, y: 320 },
  { id: 'F', x: 680, y: 320 }
]

const graphEdges = [
  { from: 'A', to: 'B', weight: 4 },
  { from: 'A', to: 'C', weight: 2 },
  { from: 'B', to: 'C', weight: 5 },
  { from: 'B', to: 'D', weight: 10 },
  { from: 'C', to: 'E', weight: 3 },
  { from: 'D', to: 'E', weight: 4 },
  { from: 'D', to: 'F', weight: 1 },
  { from: 'E', to: 'F', weight: 8 },
  { from: 'B', to: 'E', weight: 7 }
]

let visited = new Set()
let highlightNode = null
let highlightEdges = new Set()

onMounted(() => {
  resetGraph()
  crisp(graphCanvas.value, 800, 420)
})

watch(() => props.algoKey, () => {
  resetGraph()
})

const isTraversal = computed(() => props.algoKey === 'dfs' || props.algoKey === 'bfs')

const resetGraph = () => {
  stop()
  resetPlayback()
  visited.clear()
  highlightNode = null
  highlightEdges.clear()
  floydResult.value = null
  message.value = ''
  drawGraph()
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const startCurrent = () => {
  if (running.value) return
  playbackStart()
  switch (props.algoKey) {
    case 'dfs': runGraph('dfs'); break
    case 'bfs': runGraph('bfs'); break
    case 'dijkstra': runDijkstra(); break
    case 'prim': runPrim(); break
    case 'kruskal': runKruskal(); break
    case 'topological': runTopological(); break
    case 'floyd': runFloyd(); break
  }
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    startStepMode()
    switch (props.algoKey) {
      case 'dfs': runGraph('dfs'); break
      case 'bfs': runGraph('bfs'); break
      case 'dijkstra': runDijkstra(); break
      case 'prim': runPrim(); break
      case 'kruskal': runKruskal(); break
      case 'topological': runTopological(); break
      case 'floyd': runFloyd(); break
    }
  }
}

const drawGraph = () => {
  const canvas = graphCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 800, h = 420
  const c = colors.value
  ctx.fillStyle = c.bg
  ctx.fillRect(0, 0, w, h)

  graphEdges.forEach(e => {
    const from = graphNodes.find(n => n.id === e.from)
    const to = graphNodes.find(n => n.id === e.to)
    if (!from || !to) return
    const isHighlighted = highlightEdges.has(`${e.from}-${e.to}`) || highlightEdges.has(`${e.to}-${e.from}`)
    ctx.beginPath()
    ctx.moveTo(from.x, from.y)
    ctx.lineTo(to.x, to.y)
    ctx.strokeStyle = isHighlighted ? c.success : c.edge
    ctx.lineWidth = isHighlighted ? 3 : 2
    ctx.stroke()
    const mx = (from.x + to.x) / 2, my = (from.y + to.y) / 2
    ctx.fillStyle = c.danger
    ctx.font = '12px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(e.weight, mx, my - 6)
  })

  graphNodes.forEach(n => {
    ctx.beginPath()
    ctx.arc(n.x, n.y, 22, 0, Math.PI * 2)
    if (highlightNode === n.id) {
      ctx.fillStyle = c.warning; ctx.strokeStyle = c.warning
    } else if (visited.has(n.id)) {
      ctx.fillStyle = c.successLight; ctx.strokeStyle = c.success
    } else {
      ctx.fillStyle = c.primaryLight; ctx.strokeStyle = c.primary
    }
    ctx.lineWidth = 2; ctx.fill(); ctx.stroke()
    ctx.fillStyle = c.text; ctx.font = '15px sans-serif'; ctx.textAlign = 'center'
    ctx.fillText(n.id, n.x, n.y + 5)
  })
}

const getNeighbors = (nodeId) => {
  const set = new Set()
  graphEdges.forEach(e => {
    if (e.from === nodeId) set.add(e.to)
    if (e.to === nodeId) set.add(e.from)
  })
  return Array.from(set)
}

const runGraph = async (mode) => {
  if (running.value) return
  playbackStart()
  resetGraph()
  const result = []
  if (mode === 'dfs') await dfs('A', result)
  else await bfs('A', result)
  message.value = `${mode.toUpperCase()} 遍历顺序: ${result.join(' → ')}`
  highlightNode = null
  drawGraph()
  stop()
}

const dfs = async (nodeId, result) => {
  if (visited.has(nodeId) || !running.value) return
  visited.add(nodeId)
  highlightNode = nodeId
  result.push(nodeId)
  setDesc(`DFS：访问顶点 ${nodeId}，递归深入下一个未访问邻居`)
  setLine(0)
  drawGraph(); await sleep()
  for (const nb of getNeighbors(nodeId)) {
    if (!visited.has(nb)) await dfs(nb, result)
  }
  setDesc(`DFS：顶点 ${nodeId} 的邻居均已访问，回溯`)
}

const bfs = async (start, result) => {
  const queue = [start]
  visited.add(start)
  setDesc(`BFS：顶点 ${start} 入队`)
  while (queue.length > 0 && running.value) {
    const nodeId = queue.shift()
    result.push(nodeId)
    highlightNode = nodeId
    setDesc(`BFS：出队顶点 ${nodeId}，其未访问邻居依次入队`)
    setLine(0)
    drawGraph(); await sleep()
    for (const nb of getNeighbors(nodeId)) {
      if (!visited.has(nb)) {
        visited.add(nb)
        queue.push(nb)
      }
    }
  }
}

const runDijkstra = async () => {
  if (running.value) return
  playbackStart()
  resetGraph()
  const adj = {}
  graphNodes.forEach(n => adj[n.id] = [])
  graphEdges.forEach(e => {
    adj[e.from].push({ node: e.to, weight: e.weight })
    adj[e.to].push({ node: e.from, weight: e.weight })
  })
  const dist = {}, prev = {}, unvisited = new Set()
  graphNodes.forEach(n => { dist[n.id] = Infinity; unvisited.add(n.id) })
  dist['A'] = 0
  while (unvisited.size > 0 && running.value) {
    let minNode = null, minDist = Infinity
    for (const n of unvisited) {
      if (dist[n] < minDist) { minDist = dist[n]; minNode = n }
    }
    if (minNode === null) break
    unvisited.delete(minNode)
    visited.add(minNode); highlightNode = minNode
    setDesc(`从未访问顶点中选出距离最短的 ${minNode}：dist[${minNode}]=${minDist}`)
    setLine(0)
    drawGraph(); await sleep()
    for (const edge of adj[minNode]) {
      if (unvisited.has(edge.node)) {
        const nd = dist[minNode] + edge.weight
        if (nd < dist[edge.node]) {
          setDesc(`松弛边 ${minNode}-${edge.node}：dist[${edge.node}] 更新为 ${nd}`)
          dist[edge.node] = nd
          prev[edge.node] = minNode
        }
      }
    }
  }
  highlightNode = null; drawGraph()
  const parts = graphNodes.filter(n => n.id !== 'A').map(n => `A→${n.id}=${dist[n.id] === Infinity ? '∞' : dist[n.id]}`)
  message.value = 'Dijkstra 最短路径: ' + parts.join(', ')
  stop()
}

const runPrim = async () => {
  if (running.value) return
  playbackStart()
  resetGraph()
  const inTree = new Set(['A'])
  visited.add('A')
  highlightEdges.clear()
  setDesc(`Prim：从顶点 A 开始，逐步生长最小生成树`)
  drawGraph(); await sleep()

  while (inTree.size < graphNodes.length && running.value) {
    let best = null
    graphEdges.forEach(e => {
      const aIn = inTree.has(e.from), bIn = inTree.has(e.to)
      if ((aIn && !bIn) || (!aIn && bIn)) {
        if (!best || e.weight < best.weight) best = e
      }
    })
    if (!best) break
    const newNode = inTree.has(best.from) ? best.to : best.from
    inTree.add(newNode)
    visited.add(newNode)
    highlightEdges.add(`${best.from}-${best.to}`)
    setDesc(`Prim：从树中选择最小权边 ${best.from}-${best.to}（权 ${best.weight}），新顶点 ${newNode} 加入树`)
    setLine(0)
    drawGraph(); await sleep()
  }

  const total = graphEdges
    .filter(e => highlightEdges.has(`${e.from}-${e.to}`) || highlightEdges.has(`${e.to}-${e.from}`))
    .reduce((s, e) => s + e.weight, 0)
  message.value = `Prim 最小生成树已完成，总权值: ${total}`
  stop()
}

const runKruskal = async () => {
  if (running.value) return
  playbackStart()
  resetGraph()
  const sorted = [...graphEdges].sort((a, b) => a.weight - b.weight)
  const parent = {}
  graphNodes.forEach(n => parent[n.id] = n.id)
  const find = (x) => parent[x] === x ? x : (parent[x] = find(parent[x]))
  const union = (x, y) => { parent[find(x)] = find(y) }
  highlightEdges.clear()

  for (const e of sorted) {
    if (!running.value) break
    highlightNode = e.from
    setDesc(`按权值升序考察边 ${e.from}-${e.to}（权 ${e.weight}）`)
    setLine(0)
    drawGraph(); await sleep()
    if (find(e.from) !== find(e.to)) {
      union(e.from, e.to)
      visited.add(e.from); visited.add(e.to)
      highlightEdges.add(`${e.from}-${e.to}`)
      setDesc(`两端点不在同一连通分量，加入边 ${e.from}-${e.to}`)
    } else {
      setDesc(`两端点已在同一连通分量，跳过边 ${e.from}-${e.to}（会成环）`)
    }
    drawGraph(); await sleep()
  }
  highlightNode = null; drawGraph()
  const total = graphEdges
    .filter(e => highlightEdges.has(`${e.from}-${e.to}`) || highlightEdges.has(`${e.to}-${e.from}`))
    .reduce((s, e) => s + e.weight, 0)
  message.value = `Kruskal 最小生成树已完成，总权值: ${total}`
  stop()
}

const runTopological = async () => {
  if (running.value) return
  playbackStart()
  resetGraph()
  const inDegree = {}
  graphNodes.forEach(n => inDegree[n.id] = 0)
  graphEdges.forEach(e => inDegree[e.to]++)
  const queue = graphNodes.filter(n => inDegree[n.id] === 0).map(n => n.id)
  const result = []
  while (queue.length > 0 && running.value) {
    queue.sort()
    const nodeId = queue.shift()
    visited.add(nodeId); highlightNode = nodeId
    result.push(nodeId)
    setDesc(`拓扑：删除当前入度为 0 的顶点 ${nodeId}，其后继顶点入度 -1`)
    setLine(0)
    drawGraph(); await sleep()
    graphEdges.forEach(e => {
      if (e.from === nodeId) {
        inDegree[e.to]--
        if (inDegree[e.to] === 0) queue.push(e.to)
      }
    })
  }
  highlightNode = null; drawGraph()
  message.value = `拓扑排序结果: ${result.join(' → ')}`
  stop()
}

const runFloyd = async () => {
  if (running.value) return
  playbackStart()
  resetGraph()
  const ids = graphNodes.map(n => n.id)
  const n = ids.length
  const dist = Array.from({ length: n }, () => Array(n).fill(Infinity))
  for (let i = 0; i < n; i++) dist[i][i] = 0
  graphEdges.forEach(e => {
    const u = ids.indexOf(e.from), v = ids.indexOf(e.to)
    dist[u][v] = e.weight
  })
  for (let k = 0; k < n && running.value; k++) {
    highlightNode = ids[k]
    setDesc(`Floyd：以顶点 ${ids[k]} 作为中间节点，尝试更新所有点对距离`)
    setLine(0)
    for (let i = 0; i < n && running.value; i++) {
      for (let j = 0; j < n && running.value; j++) {
        if (dist[i][k] + dist[k][j] < dist[i][j]) {
          dist[i][j] = dist[i][k] + dist[k][j]
        }
      }
    }
    drawGraph(); await sleep()
  }
  highlightNode = null; drawGraph()
  floydResult.value = dist.map(row => row.map(v => v === Infinity ? '∞' : v))
  message.value = 'Floyd 全源最短路径矩阵已生成'
  stop()
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.viz-canvas { border: 1px solid var(--border); border-radius: 8px; display: block; max-width: 100%; }
.graph-msg { font-size: 13px; color: var(--text-secondary); }
.floyd-result { overflow-x: auto; }
.floyd-result table { border-collapse: collapse; font-size: 13px; }
.floyd-result td { border: 1px solid var(--border); padding: 6px 10px; text-align: center; }
</style>