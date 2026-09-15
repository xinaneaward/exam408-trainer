<template>
  <div class="viz-root">
    <div class="viz-controls">
      <template v-if="isTraversal">
        <button class="btn btn-outline btn-sm" @click="treeTraversal('pre')">前序</button>
        <button class="btn btn-outline btn-sm" @click="treeTraversal('in')">中序</button>
        <button class="btn btn-outline btn-sm" @click="treeTraversal('post')">后序</button>
        <button class="btn btn-outline btn-sm" @click="treeTraversal('level')">层次</button>
      </template>
      <template v-else>
        <input class="form-input" v-model.number="inputVal" placeholder="值" style="width:90px" type="number" />
        <button class="btn btn-primary btn-sm" @click="handleInsert">插入</button>
        <button v-if="algoKey === 'bst-search'" class="btn btn-success btn-sm" @click="handleSearch">查找</button>
        <button v-if="algoKey === 'huffman'" class="btn btn-primary btn-sm" @click="buildHuffman">构造哈夫曼树</button>
        <button v-if="algoKey === 'huffman'" class="btn btn-outline btn-sm" @click="randomWeights">随机权值</button>
      </template>
      <button class="btn btn-outline btn-sm" @click="resetTree">重置</button>
      <span v-if="message" class="tree-msg">{{ message }}</span>
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
        @reset="resetTree"
        @update:speed="v => speed.value = v"
      />
    </div>

    <canvas ref="treeCanvas" width="800" height="380" class="viz-canvas"></canvas>

    <div v-if="traversalResult" class="tree-result">
      遍历结果: <strong>{{ traversalResult }}</strong>
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
  algoKey: { type: String, default: 'bst-traversal' }
})

const { colors } = useVisualizationTheme()
const { running, paused, speed, stepMode, currentStep, start, startStepMode, switchToStepMode, pause, resume, stop, resetPlayback, step, sleep, setDesc, setLine } = usePlayback(100)
const { crisp } = useCrispCanvas()

const treeCanvas = ref(null)
const inputVal = ref('')
const traversalResult = ref('')
const message = ref('')

class TreeNode {
  constructor(val, extra = {}) {
    this.val = val
    this.left = null
    this.right = null
    this.height = 1
    this.weight = extra.weight ?? val
    this.label = extra.label ?? String(val)
  }
}

let treeRoot = null
let highlightNode = null

const isTraversal = computed(() => props.algoKey === 'bst-traversal')

onMounted(() => {
  initTree()
  crisp(treeCanvas.value, 800, 380)
})

watch(() => props.algoKey, () => {
  initTree()
})

const initTree = () => {
  stop()
  resetPlayback()
  highlightNode = null
  traversalResult.value = ''
  message.value = ''
  if (props.algoKey === 'huffman') {
    randomWeights()
    return
  }
  treeRoot = new TreeNode(50)
  treeRoot.left = new TreeNode(30)
  treeRoot.right = new TreeNode(70)
  treeRoot.left.left = new TreeNode(20)
  treeRoot.left.right = new TreeNode(40)
  treeRoot.right.left = new TreeNode(60)
  treeRoot.right.right = new TreeNode(80)
  drawTree()
}

const resetTree = () => {
  initTree()
}

const togglePause = () => {
  if (paused.value) resume()
  else pause()
}

const startCurrent = () => {
  if (!isTraversal.value) return
  start()
  treeTraversal('level')
}

const handleSwitchToStepMode = () => {
  if (running.value) {
    switchToStepMode()
  } else {
    startStepMode()
    treeTraversal('level')
  }
}

const handleInsert = async () => {
  const val = parseInt(inputVal.value)
  if (isNaN(val)) { message.value = '请输入有效数字'; return }
  if (props.algoKey === 'avl') {
    treeRoot = avlInsert(treeRoot, val)
  } else {
    treeRoot = bstInsert(treeRoot, val)
  }
  inputVal.value = ''
  message.value = `已插入 ${val}`
  highlightNode = null
  drawTree()
}

const handleSearch = async () => {
  const val = parseInt(inputVal.value)
  if (isNaN(val)) { message.value = '请输入有效数字'; return }
  start()
  traversalResult.value = ''
  const found = await bstSearch(treeRoot, val)
  message.value = found ? `找到 ${val}` : `未找到 ${val}`
  highlightNode = null
  drawTree()
  stop()
}

const bstInsert = (root, val) => {
  if (!root) return new TreeNode(val)
  if (val < root.val) root.left = bstInsert(root.left, val)
  else if (val > root.val) root.right = bstInsert(root.right, val)
  return root
}

const bstSearch = async (node, val) => {
  if (!node) return false
  highlightNode = node
  setDesc(`比较当前结点 ${node.label || node.val} 与目标 ${val}`)
  setLine(0)
  drawTree(); await sleep()
  if (val === node.val) {
    setDesc(`命中！结点 ${node.label || node.val} == ${val}`)
    return true
  }
  if (val < node.val) {
    setDesc(`${val} < ${node.val}，进入左子树查找`)
    return bstSearch(node.left, val)
  }
  setDesc(`${val} > ${node.val}，进入右子树查找`)
  return bstSearch(node.right, val)
}

const height = (node) => node ? node.height : 0
const updateHeight = (node) => {
  node.height = 1 + Math.max(height(node.left), height(node.right))
}
const balanceFactor = (node) => height(node.left) - height(node.right)

const rightRotate = (y) => {
  const x = y.left
  const T2 = x.right
  x.right = y
  y.left = T2
  updateHeight(y)
  updateHeight(x)
  return x
}
const leftRotate = (x) => {
  const y = x.right
  const T2 = y.left
  y.left = x
  x.right = T2
  updateHeight(x)
  updateHeight(y)
  return y
}

const avlInsert = (node, val) => {
  if (!node) return new TreeNode(val)
  if (val < node.val) node.left = avlInsert(node.left, val)
  else if (val > node.val) node.right = avlInsert(node.right, val)
  else return node
  updateHeight(node)
  const bf = balanceFactor(node)
  if (bf > 1 && val < node.left.val) return rightRotate(node)
  if (bf < -1 && val > node.right.val) return leftRotate(node)
  if (bf > 1 && val > node.left.val) {
    node.left = leftRotate(node.left)
    return rightRotate(node)
  }
  if (bf < -1 && val < node.right.val) {
    node.right = rightRotate(node.right)
    return leftRotate(node)
  }
  return node
}

const randomWeights = () => {
  const chars = ['A', 'B', 'C', 'D', 'E', 'F', 'G']
  const weights = chars.map(c => ({ char: c, weight: Math.floor(Math.random() * 20) + 5 }))
  buildHuffmanTree(weights)
  message.value = '已生成随机权值'
}

const buildHuffman = () => {
  randomWeights()
}

const buildHuffmanTree = (items) => {
  const nodes = items.map(it => new TreeNode(it.weight, { weight: it.weight, label: it.char }))
  while (nodes.length > 1) {
    nodes.sort((a, b) => a.weight - b.weight)
    const left = nodes.shift()
    const right = nodes.shift()
    const parent = new TreeNode(left.weight + right.weight, { weight: left.weight + right.weight, label: left.weight + '+' + right.weight })
    parent.left = left
    parent.right = right
    nodes.push(parent)
  }
  treeRoot = nodes[0] || null
  drawTree()
}

const getTreeHeight = (node) => {
  if (!node) return 0
  return 1 + Math.max(getTreeHeight(node.left), getTreeHeight(node.right))
}

const drawTree = () => {
  const canvas = treeCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  const w = 800, h = 380
  const c = colors.value
  ctx.fillStyle = c.bg
  ctx.fillRect(0, 0, w, h)
  if (!treeRoot) return
  const height = getTreeHeight(treeRoot)

  const drawNode = (node, x, y, level, offset) => {
    if (!node) return
    const radius = 20
    const dx = offset / 2
    if (node.left) {
      ctx.beginPath(); ctx.moveTo(x, y + radius); ctx.lineTo(x - dx, y + 60)
      ctx.strokeStyle = c.edge; ctx.lineWidth = 2; ctx.stroke()
    }
    if (node.right) {
      ctx.beginPath(); ctx.moveTo(x, y + radius); ctx.lineTo(x + dx, y + 60)
      ctx.strokeStyle = c.edge; ctx.lineWidth = 2; ctx.stroke()
    }
    if (node.left) drawNode(node.left, x - dx, y + 60, level + 1, dx)
    if (node.right) drawNode(node.right, x + dx, y + 60, level + 1, dx)

    ctx.beginPath(); ctx.arc(x, y, radius, 0, Math.PI * 2)
    if (highlightNode === node) {
      ctx.fillStyle = c.warning; ctx.strokeStyle = c.warning
    } else {
      ctx.fillStyle = c.primaryLight; ctx.strokeStyle = c.primary
    }
    ctx.lineWidth = 2; ctx.fill(); ctx.stroke()
    ctx.fillStyle = c.text; ctx.font = '13px sans-serif'; ctx.textAlign = 'center'
    ctx.fillText(node.label || String(node.val), x, y + 4)
  }

  const initialOffset = Math.pow(2, Math.max(0, height - 2)) * 40
  drawNode(treeRoot, w / 2, 35, 0, Math.min(initialOffset, 360))
}

const treeTraversal = async (mode) => {
  if (!treeRoot || running.value) return
  start()
  setDesc(`开始${mode === 'pre' ? '前序' : mode === 'in' ? '中序' : mode === 'post' ? '后序' : '层次'}遍历`)
  traversalResult.value = ''
  const result = []
  if (mode === 'pre') await preOrder(treeRoot, result)
  else if (mode === 'in') await inOrder(treeRoot, result)
  else if (mode === 'post') await postOrder(treeRoot, result)
  else if (mode === 'level') await levelOrder(result)
  traversalResult.value = result.join(' → ')
  setDesc('遍历完成')
  highlightNode = null
  drawTree()
  stop()
}

const preOrder = async (node, result) => {
  if (!node || !running.value) return
  result.push(node.label || node.val)
  highlightNode = node; drawTree(); await sleep()
  setDesc(`前序：访问结点 ${node.label || node.val}，然后递归左子树`)
  setLine(0)
  await preOrder(node.left, result)
  setDesc(`前序：回到 ${node.label || node.val}，递归右子树`)
  await preOrder(node.right, result)
}
const inOrder = async (node, result) => {
  if (!node || !running.value) return
  await inOrder(node.left, result)
  result.push(node.label || node.val)
  highlightNode = node; drawTree(); await sleep()
  setDesc(`中序：先递归左子树，再访问结点 ${node.label || node.val}`)
  setLine(1)
  await inOrder(node.right, result)
}
const postOrder = async (node, result) => {
  if (!node || !running.value) return
  await postOrder(node.left, result)
  await postOrder(node.right, result)
  result.push(node.label || node.val)
  highlightNode = node; drawTree(); await sleep()
  setDesc(`后序：先递归左右子树，最后访问结点 ${node.label || node.val}`)
  setLine(2)
}
const levelOrder = async (result) => {
  if (!treeRoot) return
  const queue = [treeRoot]
  while (queue.length > 0 && running.value) {
    const node = queue.shift()
    result.push(node.label || node.val)
    highlightNode = node; drawTree(); await sleep()
    setDesc(`层次：出队访问结点 ${node.label || node.val}，其子结点入队`)
    setLine(0)
    if (node.left) queue.push(node.left)
    if (node.right) queue.push(node.right)
  }
}
</script>

<style scoped>
.viz-root { display: flex; flex-direction: column; gap: 12px; }
.viz-controls { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
.viz-canvas { border: 1px solid var(--border); border-radius: 8px; display: block; max-width: 100%; }
.tree-msg { font-size: 13px; color: var(--text-secondary); }
.tree-result { font-size: 14px; color: var(--text); }
</style>