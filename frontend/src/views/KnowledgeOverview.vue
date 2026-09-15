<template>
  <div class="overview-page">
    <TopNav :username="user?.nickname || '用户'" @toggle-theme="toggleTheme" @logout="logout" />
    
    <div class="overview-body">
      <aside class="overview-sidebar">
        <div class="sidebar-header">
          <h3>知识点目录</h3>
          <label class="sidebar-toggle">
            <input type="checkbox" v-model="showProgress" />
            <span>显示进度</span>
          </label>
        </div>
        
        <div class="search-box">
          <input v-model="searchKeyword" placeholder="搜索知识点..." />
        </div>
        
        <div class="knowledge-tree">
          <div v-for="subject in knowledgeTree" :key="subject.name" class="tree-subject">
            <div 
              class="tree-subject-header" 
              :class="{ expanded: expandedSubjects[subject.name] }"
              @click="toggleSubject(subject.name)"
            >
              <span class="tree-arrow">{{ expandedSubjects[subject.name] ? '▼' : '▶' }}</span>
              <span class="tree-subject-name">{{ subject.name }}</span>
            </div>
            <div v-if="expandedSubjects[subject.name]" class="tree-chapters">
              <div v-for="chapter in subject.children" :key="chapter.name" class="tree-chapter">
                <div 
                  class="tree-chapter-header"
                  :class="{ expanded: expandedChapters[chapter.name] }"
                  @click="toggleChapter(chapter.name)"
                >
                  <span class="tree-arrow">{{ expandedChapters[chapter.name] ? '▼' : '▶' }}</span>
                  <span class="tree-chapter-name">{{ chapter.name }}</span>
                </div>
                <div v-if="expandedChapters[chapter.name]" class="tree-points">
                  <div 
                    v-for="point in filterPoints(chapter.children)" 
                    :key="point.name"
                    class="tree-point"
                    :class="{ active: selectedPoint?.name === point.name }"
                    @click="selectPoint(point)"
                  >
                    <span v-if="showProgress" class="point-dot" :class="getPointStatus(point)"></span>
                    <span class="point-name">{{ point.name }}</span>
                    <span v-if="point.count" class="point-count">{{ point.count }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <main class="overview-main">
        <div class="overview-header">
          <h2>真题掌握度热力矩阵</h2>
          <div class="legend">
            <span class="legend-item"><span class="legend-dot dot-white"></span>未答</span>
            <span class="legend-item"><span class="legend-dot dot-green"></span>已掌握</span>
            <span class="legend-item"><span class="legend-dot dot-yellow"></span>不熟</span>
            <span class="legend-item"><span class="legend-dot dot-red"></span>不会</span>
          </div>
        </div>

        <div class="heatmap-container">
          <div class="heatmap-header">
            <div class="hm-year-label">年份</div>
            <div v-for="subject in ['数据结构', '计组', '操作系统', '计算机网络']" :key="subject" class="hm-subject-label">
              {{ subject }}
            </div>
          </div>
          
          <div v-for="year in years" :key="year" class="heatmap-row">
            <div class="hm-year" @click="startYearExam(year)">{{ year }}</div>
            <div 
              v-for="(subject, idx) in subjects" 
              :key="subject"
              class="hm-cell"
              :class="getCellClass(year, subject)"
              @click="startYearExam(year)"
              :title="getCellTitle(year, subject)"
            >
              <span>{{ getCellPercent(year, subject) }}%</span>
            </div>
          </div>
        </div>

        <div v-if="selectedPoint" class="selected-info">
          <h4>选中知识点: {{ selectedPoint.name }}</h4>
          <p>包含 {{ selectedPoint.count || 0 }} 道题目</p>
          <button class="btn btn-primary" @click="startPointPractice">开始练习</button>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopNav from '../components/TopNav.vue'
import api from '../api'

const router = useRouter()
const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const searchKeyword = ref('')
const showProgress = ref(true)
const knowledgeTree = ref([])
const selectedPoint = ref(null)
const expandedSubjects = reactive({})
const expandedChapters = reactive({})

const years = [2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017, 2016, 2015, 2014, 2013, 2012, 2011, 2010, 2009]
const subjects = ['数据结构', '计算机组成原理', '操作系统', '计算机网络']

const defaultKnowledgeTree = [
  {
    name: '数据结构',
    children: [
      {
        name: '第一章 绪论',
        children: [
          { name: '数据结构基本概念', count: 5, mastered: 0 },
          { name: '算法与算法评价', count: 8, mastered: 0 },
          { name: '时间复杂度', count: 12, mastered: 0 },
          { name: '空间复杂度', count: 6, mastered: 0 }
        ]
      },
      {
        name: '第二章 线性表',
        children: [
          { name: '线性表定义与操作', count: 10, mastered: 0 },
          { name: '顺序存储结构', count: 8, mastered: 0 },
          { name: '链式存储结构', count: 15, mastered: 0 },
          { name: '单链表', count: 12, mastered: 0 },
          { name: '双链表', count: 8, mastered: 0 },
          { name: '循环链表', count: 6, mastered: 0 }
        ]
      },
      {
        name: '第三章 栈和队列',
        children: [
          { name: '栈的定义与操作', count: 10, mastered: 0 },
          { name: '顺序栈', count: 8, mastered: 0 },
          { name: '链栈', count: 6, mastered: 0 },
          { name: '队列的定义与操作', count: 10, mastered: 0 },
          { name: '循环队列', count: 12, mastered: 0 },
          { name: '链队列', count: 8, mastered: 0 }
        ]
      },
      {
        name: '第四章 树与二叉树',
        children: [
          { name: '树的基本概念', count: 12, mastered: 0 },
          { name: '二叉树定义与性质', count: 15, mastered: 0 },
          { name: '二叉树的存储结构', count: 10, mastered: 0 },
          { name: '二叉树的遍历', count: 18, mastered: 0 },
          { name: '线索二叉树', count: 8, mastered: 0 },
          { name: '哈夫曼树与编码', count: 10, mastered: 0 }
        ]
      },
      {
        name: '第五章 图',
        children: [
          { name: '图的基本概念', count: 10, mastered: 0 },
          { name: '图的存储结构', count: 12, mastered: 0 },
          { name: '图的遍历', count: 15, mastered: 0 },
          { name: '最小生成树', count: 10, mastered: 0 },
          { name: '最短路径', count: 12, mastered: 0 },
          { name: '拓扑排序', count: 8, mastered: 0 }
        ]
      },
      {
        name: '第六章 查找',
        children: [
          { name: '查找基本概念', count: 8, mastered: 0 },
          { name: '顺序与折半查找', count: 10, mastered: 0 },
          { name: 'B树与B+树', count: 12, mastered: 0 },
          { name: '散列表', count: 10, mastered: 0 },
          { name: 'KMP算法', count: 8, mastered: 0 }
        ]
      },
      {
        name: '第七章 排序',
        children: [
          { name: '排序基本概念', count: 8, mastered: 0 },
          { name: '插入排序', count: 10, mastered: 0 },
          { name: '快速排序', count: 15, mastered: 0 },
          { name: '堆排序', count: 12, mastered: 0 },
          { name: '归并排序', count: 10, mastered: 0 },
          { name: '基数排序', count: 6, mastered: 0 }
        ]
      }
    ]
  },
  {
    name: '计算机组成原理',
    children: [
      {
        name: '第一章 计算机系统概述',
        children: [
          { name: '计算机发展历程', count: 5, mastered: 0 },
          { name: '系统层次结构', count: 8, mastered: 0 },
          { name: '性能指标', count: 10, mastered: 0 },
          { name: 'Amdahl定律', count: 6, mastered: 0 }
        ]
      },
      {
        name: '第二章 数据表示和运算',
        children: [
          { name: '数制与编码', count: 10, mastered: 0 },
          { name: '定点数表示', count: 12, mastered: 0 },
          { name: '浮点数表示', count: 10, mastered: 0 },
          { name: 'IEEE 754标准', count: 8, mastered: 0 },
          { name: '算术逻辑单元', count: 10, mastered: 0 }
        ]
      },
      {
        name: '第三章 存储系统',
        children: [
          { name: '存储器分类', count: 8, mastered: 0 },
          { name: '层次结构', count: 10, mastered: 0 },
          { name: 'Cache基本原理', count: 15, mastered: 0 },
          { name: 'Cache映射', count: 12, mastered: 0 },
          { name: '虚拟存储器', count: 10, mastered: 0 }
        ]
      },
      {
        name: '第四章 指令系统',
        children: [
          { name: '指令格式', count: 10, mastered: 0 },
          { name: '寻址方式', count: 12, mastered: 0 },
          { name: 'CISC与RISC', count: 8, mastered: 0 }
        ]
      },
      {
        name: '第五章 中央处理器',
        children: [
          { name: 'CPU功能与结构', count: 12, mastered: 0 },
          { name: '指令执行过程', count: 10, mastered: 0 },
          { name: '数据通路', count: 8, mastered: 0 },
          { name: '指令流水线', count: 15, mastered: 0 },
          { name: '流水线冒险', count: 10, mastered: 0 }
        ]
      },
      {
        name: '第六章 总线',
        children: [
          { name: '总线概述', count: 6, mastered: 0 },
          { name: '总线仲裁', count: 8, mastered: 0 },
          { name: '总线操作与定时', count: 6, mastered: 0 }
        ]
      },
      {
        name: '第七章 输入输出系统',
        children: [
          { name: 'I/O系统概述', count: 8, mastered: 0 },
          { name: '程序查询方式', count: 6, mastered: 0 },
          { name: '程序中断方式', count: 10, mastered: 0 },
          { name: 'DMA方式', count: 8, mastered: 0 }
        ]
      }
    ]
  },
  {
    name: '操作系统',
    children: [
      {
        name: '第一章 操作系统概述',
        children: [
          { name: '概念与特征', count: 8, mastered: 0 },
          { name: '操作系统发展', count: 6, mastered: 0 },
          { name: '运行环境', count: 8, mastered: 0 },
          { name: '系统调用', count: 6, mastered: 0 }
        ]
      },
      {
        name: '第二章 进程管理',
        children: [
          { name: '进程概念与特征', count: 10, mastered: 0 },
          { name: '进程状态与转换', count: 12, mastered: 0 },
          { name: '进程同步', count: 15, mastered: 0 },
          { name: '信号量', count: 12, mastered: 0 },
          { name: '死锁', count: 10, mastered: 0 },
          { name: '银行家算法', count: 8, mastered: 0 }
        ]
      },
      {
        name: '第三章 内存管理',
        children: [
          { name: '内存管理概念', count: 8, mastered: 0 },
          { name: '连续分配方式', count: 6, mastered: 0 },
          { name: '分页存储管理', count: 12, mastered: 0 },
          { name: '分段存储管理', count: 10, mastered: 0 },
          { name: '虚拟内存', count: 15, mastered: 0 },
          { name: '页面置换算法', count: 12, mastered: 0 }
        ]
      },
      {
        name: '第四章 文件管理',
        children: [
          { name: '文件系统基础', count: 8, mastered: 0 },
          { name: '目录结构', count: 10, mastered: 0 },
          { name: '文件共享与保护', count: 8, mastered: 0 },
          { name: '磁盘组织与管理', count: 10, mastered: 0 }
        ]
      },
      {
        name: '第五章 输入输出管理',
        children: [
          { name: 'I/O管理概述', count: 6, mastered: 0 },
          { name: 'I/O控制方式', count: 8, mastered: 0 },
          { name: 'SPOOLing技术', count: 8, mastered: 0 },
          { name: '缓冲管理', count: 10, mastered: 0 }
        ]
      }
    ]
  },
  {
    name: '计算机网络',
    children: [
      {
        name: '第一章 网络体系结构',
        children: [
          { name: '网络概述', count: 8, mastered: 0 },
          { name: 'OSI参考模型', count: 10, mastered: 0 },
          { name: 'TCP/IP模型', count: 10, mastered: 0 },
          { name: '性能指标', count: 8, mastered: 0 }
        ]
      },
      {
        name: '第二章 物理层',
        children: [
          { name: '通信基础', count: 10, mastered: 0 },
          { name: '奈奎斯特与香农定理', count: 8, mastered: 0 },
          { name: '编码与调制', count: 8, mastered: 0 },
          { name: '传输介质', count: 6, mastered: 0 }
        ]
      },
      {
        name: '第三章 数据链路层',
        children: [
          { name: '数据链路层功能', count: 10, mastered: 0 },
          { name: '差错控制', count: 8, mastered: 0 },
          { name: '流量控制', count: 12, mastered: 0 },
          { name: 'CSMA/CD', count: 10, mastered: 0 },
          { name: '以太网', count: 10, mastered: 0 }
        ]
      },
      {
        name: '第四章 网络层',
        children: [
          { name: '网络层功能', count: 10, mastered: 0 },
          { name: 'IPv4', count: 15, mastered: 0 },
          { name: 'IPv6', count: 8, mastered: 0 },
          { name: '路由协议', count: 12, mastered: 0 },
          { name: 'ARP/DHCP/ICMP', count: 10, mastered: 0 }
        ]
      },
      {
        name: '第五章 传输层',
        children: [
          { name: '传输层功能', count: 8, mastered: 0 },
          { name: 'UDP协议', count: 6, mastered: 0 },
          { name: 'TCP协议', count: 15, mastered: 0 },
          { name: 'TCP连接管理', count: 12, mastered: 0 },
          { name: 'TCP拥塞控制', count: 10, mastered: 0 }
        ]
      },
      {
        name: '第六章 应用层',
        children: [
          { name: 'DNS系统', count: 8, mastered: 0 },
          { name: 'FTP协议', count: 6, mastered: 0 },
          { name: '电子邮件', count: 8, mastered: 0 },
          { name: 'HTTP协议', count: 12, mastered: 0 }
        ]
      }
    ]
  }
]

function toggleTheme() {
  document.body.classList.toggle('dark-mode')
}

function logout() {
  try { api.logout() } catch (e) {}
  localStorage.removeItem('user')
  router.push('/login')
}

function toggleSubject(name) {
  expandedSubjects[name] = !expandedSubjects[name]
}

function toggleChapter(name) {
  expandedChapters[name] = !expandedChapters[name]
}

function filterPoints(points) {
  if (!searchKeyword.value) return points || []
  const kw = searchKeyword.value.toLowerCase()
  return (points || []).filter(p => p.name.toLowerCase().includes(kw))
}

function selectPoint(point) {
  selectedPoint.value = selectedPoint.value?.name === point.name ? null : point
}

function getPointStatus(point) {
  const count = point.count || 0
  if (count === 0) return 'dot-white'
  const mastered = point.mastered || 0
  if (mastered >= 80) return 'dot-green'
  if (mastered >= 50) return 'dot-yellow'
  return 'dot-red'
}

const subjectQuestionCounts = {
  '数据结构': 47,
  '计算机组成原理': 47,
  '操作系统': 47,
  '计算机网络': 47
}

function getCellClass(year, subject) {
  const percent = getCellPercent(year, subject)
  if (percent >= 80) return 'hm-cell-1'
  if (percent >= 50) return 'hm-cell-2'
  if (percent >= 20) return 'hm-cell-3'
  return 'hm-cell-0'
}

function getCellPercent(year, subject) {
  const basePercents = {
    2025: { '数据结构': 100, '计算机组成原理': 100, '操作系统': 100, '计算机网络': 100 },
    2024: { '数据结构': 95, '计算机组成原理': 95, '操作系统': 95, '计算机网络': 95 },
    2023: { '数据结构': 90, '计算机组成原理': 90, '操作系统': 90, '计算机网络': 90 },
    2022: { '数据结构': 85, '计算机组成原理': 85, '操作系统': 85, '计算机网络': 85 },
    2021: { '数据结构': 80, '计算机组成原理': 80, '操作系统': 80, '计算机网络': 80 },
    2020: { '数据结构': 75, '计算机组成原理': 75, '操作系统': 75, '计算机网络': 75 },
    2019: { '数据结构': 70, '计算机组成原理': 70, '操作系统': 70, '计算机网络': 70 },
    2018: { '数据结构': 65, '计算机组成原理': 65, '操作系统': 65, '计算机网络': 65 },
    2017: { '数据结构': 60, '计算机组成原理': 60, '操作系统': 60, '计算机网络': 60 },
    2016: { '数据结构': 55, '计算机组成原理': 55, '操作系统': 55, '计算机网络': 55 },
    2015: { '数据结构': 50, '计算机组成原理': 50, '操作系统': 50, '计算机网络': 50 },
    2014: { '数据结构': 45, '计算机组成原理': 45, '操作系统': 45, '计算机网络': 45 },
    2013: { '数据结构': 40, '计算机组成原理': 40, '操作系统': 40, '计算机网络': 40 },
    2012: { '数据结构': 35, '计算机组成原理': 35, '操作系统': 35, '计算机网络': 35 },
    2011: { '数据结构': 30, '计算机组成原理': 30, '操作系统': 30, '计算机网络': 30 }
  }
  return basePercents[year]?.[subject] || Math.floor(Math.random() * 30) + 20
}

function getCellTitle(year, subject) {
  const count = subjectQuestionCounts[subject] || 47
  const percent = getCellPercent(year, subject)
  const questionCount = Math.round(count * percent / 100)
  return `${year}年 ${subject}\n共${count}题，占比${percent}%（约${questionCount}题）`
}

function startYearExam(year) {
  router.push({ path: '/practice', query: { year } })
}

function startPointPractice() {
  if (selectedPoint.value) {
    router.push({ 
      path: '/practice', 
      query: { 
        knowledgeTag: selectedPoint.value.name,
        subject: getPointSubject(selectedPoint.value)
      } 
    })
  }
}

function getPointSubject(point) {
  for (const subject of knowledgeTree.value) {
    for (const chapter of subject.children) {
      if (chapter.children && chapter.children.find(p => p.name === point.name)) {
        return subject.name
      }
    }
  }
  return '数据结构'
}

onMounted(() => {
  knowledgeTree.value = defaultKnowledgeTree
  if (knowledgeTree.value.length) {
    expandedSubjects[knowledgeTree.value[0].name] = true
    if (knowledgeTree.value[0].children?.length) {
      expandedChapters[knowledgeTree.value[0].children[0].name] = true
    }
  }
})
</script>

<style scoped>
.overview-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.overview-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.overview-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.sidebar-header h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.sidebar-toggle {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.search-box {
  padding: 12px;
}

.search-box input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 13px;
  box-sizing: border-box;
}

.knowledge-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.tree-subject {
  margin-bottom: 4px;
}

.tree-subject-header,
.tree-chapter-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 6px;
  font-size: 13px;
}

.tree-subject-header:hover,
.tree-chapter-header:hover {
  background: #f1f3f4;
}

.tree-subject-header {
  font-weight: 600;
}

.tree-arrow {
  font-size: 10px;
  color: #999;
  flex-shrink: 0;
}

.tree-subject-name {
  flex: 1;
}

.tree-chapters {
  padding-left: 12px;
}

.tree-chapter-header {
  font-size: 12px;
  color: #5f6368;
}

.tree-points {
  padding-left: 12px;
}

.tree-point {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 10px;
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  color: #3c4043;
}

.tree-point:hover {
  background: #e8f0fe;
}

.tree-point.active {
  background: #e8f0fe;
  font-weight: 600;
}

.point-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-white { background: #e0e0e0; }
.dot-green { background: #4caf50; }
.dot-yellow { background: #ff9800; }
.dot-red { background: #f44336; }

.point-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.point-count {
  font-size: 11px;
  color: #999;
  background: #f1f3f4;
  padding: 1px 6px;
  border-radius: 10px;
}

.overview-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.overview-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.legend {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #666;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 4px;
  display: inline-block;
}

.legend-dot.dot-white { background: #fff; border: 1px solid #e0e0e0; }
.legend-dot.dot-green { background: #9be9a8; }
.legend-dot.dot-yellow { background: #ffd666; }
.legend-dot.dot-red { background: #ff9c9c; }

.heatmap-container {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e0e0e0;
  overflow: hidden;
}

.heatmap-header {
  display: grid;
  grid-template-columns: 60px repeat(4, 1fr);
  padding: 12px;
  background: #f8f9fa;
  border-bottom: 1px solid #e0e0e0;
}

.hm-year-label {
  font-weight: 600;
  font-size: 13px;
}

.hm-subject-label {
  text-align: center;
  font-size: 13px;
  font-weight: 500;
}

.heatmap-row {
  display: grid;
  grid-template-columns: 60px repeat(4, 1fr);
  border-bottom: 1px solid #e0e0e0;
}

.heatmap-row:last-child {
  border-bottom: none;
}

.hm-year {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  background: #f8f9fa;
}

.hm-year:hover {
  background: #e8f0fe;
  color: #1a73e8;
}

.hm-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.15s;
}

.hm-cell:hover {
  box-shadow: inset 0 0 0 2px #1a73e8;
}

.hm-cell-0 { background: #fff; color: #999; }
.hm-cell-1 { background: #e8f5e9; color: #2e7d32; }
.hm-cell-2 { background: #fff3e0; color: #e65100; }
.hm-cell-3 { background: #ffebee; color: #c62828; }

.selected-info {
  margin-top: 24px;
  padding: 16px;
  background: #e8f0fe;
  border-radius: 12px;
  border: 1px solid #1a73e8;
}

.selected-info h4 {
  margin: 0 0 8px;
  color: #1a73e8;
}

.selected-info p {
  margin: 0 0 12px;
  font-size: 14px;
}

.btn {
  padding: 8px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.btn-primary {
  background: #1a73e8;
  color: #fff;
}

.btn-primary:hover {
  background: #1557b0;
}
</style>
