import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/', name: 'Home', component: () => import('../views/Home.vue'), meta: { auth: true } },
  { path: '/practice', name: 'Practice', component: () => import('../views/Practice.vue'), meta: { auth: true } },
  { path: '/wrong', name: 'Wrong', component: () => import('../views/Wrong.vue'), meta: { auth: true } },
  { path: '/stats', name: 'Stats', component: () => import('../views/Stats.vue'), meta: { auth: true } },
  { path: '/pdf-viewer', name: 'PdfViewer', component: () => import('../views/PdfViewer.vue'), meta: { auth: true } },
  { path: '/visualization', name: 'AlgorithmList', component: () => import('../views/visualization/AlgorithmList.vue'), meta: { auth: true } },
  { path: '/visualization/:category/:algoKey', name: 'AlgorithmDemo', component: () => import('../views/visualization/AlgorithmDemo.vue'), meta: { auth: true } },
  { path: '/overview', name: 'Overview', component: () => import('../views/KnowledgeOverview.vue'), meta: { auth: true } },
  { path: '/smart-exam', name: 'SmartExam', component: () => import('../views/SmartExam.vue'), meta: { auth: true } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = localStorage.getItem('user')
  if (to.meta.auth && !user) {
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && user) {
    next('/')
  } else {
    next()
  }
})

export default router
