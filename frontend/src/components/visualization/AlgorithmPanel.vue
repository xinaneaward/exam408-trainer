<template>
  <div class="panel-root card">
    <div class="panel-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="panel-tab"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >{{ tab.label }}<span v-if="tab.key !== 'complexity' && live.active && live.line >= 0" class="tab-live">●</span></button>
    </div>

    <div v-if="activeTab === 'complexity'" class="panel-body">
      <div class="info-row"><span>时间复杂度</span><strong>{{ algo.time }}</strong></div>
      <div class="info-row"><span>空间复杂度</span><strong>{{ algo.space }}</strong></div>
      <div class="info-row"><span>稳定性</span><strong>{{ algo.stable }}</strong></div>
      <div class="info-row"><span>难度</span><strong>{{ algo.difficulty }}</strong></div>
    </div>

    <div v-if="activeTab === 'steps'" class="panel-body">
      <ol>
        <li v-for="(step, i) in algo.steps" :key="i">{{ step }}</li>
      </ol>
    </div>

    <div v-if="activeTab === 'pseudo'" class="panel-body">
      <pre class="pseudo-code">
        <div
          v-for="(ln, i) in pseudoLines"
          :key="i"
          class="code-line"
          :class="{ hl: live.active && live.line === i }"
        >{{ ln }}</div>
      </pre>
      <div v-if="live.active && live.desc" class="live-line">当前执行: {{ live.desc }}</div>
    </div>

    <div v-if="activeTab === 'code'" class="panel-body">
      <pre class="code-block">
        <div
          v-for="(ln, i) in codeLines"
          :key="i"
          class="code-line"
          :class="{ 'hl-dark': live.active && live.line === i }"
        >{{ ln }}</div>
      </pre>
      <div v-if="live.active && live.desc" class="live-line">当前执行: {{ live.desc }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { vizLive } from '../../composables/vizLive.js'

const props = defineProps({
  algo: { type: Object, required: true }
})

const tabs = [
  { key: 'complexity', label: '复杂度' },
  { key: 'steps', label: '步骤说明' },
  { key: 'pseudo', label: '伪代码' },
  { key: 'code', label: 'C语言代码' }
]
const activeTab = ref('complexity')
const live = vizLive

const pseudoLines = computed(() => (props.algo.pseudo ?? '').split('\n'))
const codeLines = computed(() => (props.algo.code ?? '').split('\n'))
</script>

<style scoped>
.panel-root { margin-top: 16px; }
.panel-tabs { display: flex; gap: 8px; margin-bottom: 12px; border-bottom: 1px solid var(--border); padding-bottom: 8px; flex-wrap: wrap; }
.panel-tab { padding: 6px 14px; border-radius: 6px; border: 1px solid var(--border); background: transparent; color: var(--text-secondary); cursor: pointer; font-size: 14px; transition: all 0.2s; }
.panel-tab:hover { color: var(--primary); background: rgba(5, 150, 105, 0.08); border-color: var(--primary); transform: scale(1.06); }
.panel-tab.active { color: var(--primary); background: rgba(5, 150, 105, 0.12); font-weight: 600; border-color: var(--primary); transform: scale(1); }
.tab-live { font-size: 10px; color: var(--primary); margin-left: 4px; vertical-align: middle; }
.panel-body { font-size: 14px; line-height: 1.7; }
.info-row { display: flex; justify-content: space-between; max-width: 320px; padding: 6px 0; border-bottom: 1px dashed var(--border); }
.info-row:last-child { border-bottom: none; }
.pseudo-code { background: var(--bg); padding: 14px; border-radius: 8px; overflow-x: auto; font-family: Consolas, 'Courier New', monospace; color: var(--text); margin: 0; }
.code-block { background: #1e1e1e; padding: 16px; border-radius: 8px; overflow-x: auto; font-family: Consolas, 'Courier New', monospace; color: #d4d4d4; font-size: 13px; line-height: 1.5; margin: 0; }
.code-block::before { content: '// C语言代码'; display: block; color: #808080; margin-bottom: 12px; font-size: 12px; }
.code-line { white-space: pre; }
.code-line.hl { background: rgba(245, 158, 11, 0.25); border-left: 3px solid var(--warning); padding: 0 6px; color: var(--warning); font-weight: 600; }
.code-block .code-line.hl-dark { background: rgba(202, 240, 248, 0.14); border-left: 3px solid #4fc3f7; padding: 0 6px; color: #4fc3f7; }
.live-line { margin-top: 10px; padding: 8px 12px; border-radius: 8px; background: rgba(5, 150, 105, 0.1); border: 1px solid var(--primary); color: var(--text); font-size: 13px; }
ol { padding-left: 20px; }
li { margin-bottom: 6px; }
</style>