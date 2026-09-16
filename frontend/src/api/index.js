import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  withCredentials: true
})

api.interceptors.response.use(
  r => r,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('user')
      if (!['/login', '/register'].includes(window.location.pathname)) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(err)
  }
)

export default {
  login: (data) => api.post('/auth/login', data),
  register: (data) => api.post('/auth/register', data),
  logout: () => api.post('/auth/logout'),
  getUser: () => api.get('/auth/user'),

  getYears: () => api.get('/question/years'),
  getSubjects: () => api.get('/question/subjects'),
  listQuestions: (params) => api.get('/question/list', { params }),
  randomQuestions: (limit) => api.get('/question/random', { params: { limit } }),
  getQuestion: (id) => api.get(`/question/${id}`),
  filterQuestions: (params) => api.get('/question/filter', { params }),

  submitAnswers: (data) => api.post('/exam/submit', data),
  saveAnswer: (data) => api.post('/exam/save-answer', data),
  getHistory: () => api.get('/exam/history'),
  getExamDetail: (id) => api.get(`/exam/detail/${id}`),

  getWrongList: (params) => api.get('/wrong/list', { params }),
  getWrongPaper: (params) => api.get('/wrong/paper', { params }),
  removeWrong: (id) => api.delete(`/wrong/${id}`),
  markReviewed: (id) => api.put(`/wrong/${id}/review`),
  recordReviewResult: (id, correct) => api.post(`/wrong/${id}/review-result`, { correct }),
  getWrongCount: () => api.get('/wrong/count'),
  setWrongReason: (id, reason) => api.put(`/wrong/${id}/reason`, { reason }),
  getReasonStats: () => api.get('/wrong/reason-stats'),
  getReviewToday: () => api.get('/wrong/review-today'),

  getStats: () => api.get('/stats/overview'),
  getDashboard: () => api.get('/stats/dashboard'),
  getHeatmap: () => api.get('/stats/heatmap'),
  getMastery: () => api.get('/stats/mastery'),
  getCalendar: () => api.get('/stats/calendar'),
  getCountdown: () => api.get('/stats/countdown'),
  getMonthlyReport: (month) => api.get('/stats/monthly-report', { params: { month } }),

  // AI 学情诊断
  getDiagnosis: () => api.post('/ai/diagnosis'),

  // AI 变式出题
  generateVariant: (data) => api.post('/ai/variant/generate', data),
  listVariants: (subject) => api.get('/ai/variant/list', { params: subject ? { subject } : {} }),

  // Overview (knowledge point matrix)
  getOverview: (year) => api.get('/overview', { params: { year } }),

  // Knowledge tree
  getKnowledgeTree: () => api.get('/question/knowledge-tree'),
  getKnowledgeTags: (subject) => api.get('/question/knowledge-tags', { params: { subject } })
}
