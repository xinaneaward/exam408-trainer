<template>
  <div class="practice-page" :class="{ 'exam-mode': phase === 'exam' }">
    <header v-if="phase !== 'exam'" class="top-nav">
      <div class="nav-left">
        <span class="logo">408真题训练</span>
      </div>
      <nav class="nav-center">
        <router-link to="/">首页</router-link>
        <router-link to="/overview">知识总览</router-link>
        <router-link to="/practice" class="active">刷题练习</router-link>
        <router-link to="/wrong">错题本</router-link>
        <router-link to="/stats">学习统计</router-link>
        <router-link to="/visualization">算法可视化</router-link>
      </nav>
      <div class="nav-right">
        <span class="nav-nickname">{{ user?.nickname || '用户' }}</span>
        <button class="nav-logout-btn" @click="logout">退出</button>
        <button class="nav-theme-btn" @click="toggleTheme" title="切换主题">🌓</button>
      </div>
    </header>

    <main class="practice-main">
      <template v-if="phase === 'mode_select'">
        <div class="page-header"><h2>刷题练习</h2></div>
        <div v-if="resumeInfo" class="resume-banner">
          <div class="resume-info">
            <div class="resume-title">📌 有未完成的专项练习</div>
            <div class="resume-desc">{{ resumeInfo.subject }} · {{ resumeInfo.knowledgeTag || (resumeInfo.type === '综合应用' ? '综合应用' : '单选') }} · 已答 {{ resumeInfo.answered }}/{{ resumeInfo.total }} 题</div>
          </div>
          <div style="display:flex;gap:8px;">
            <button class="btn btn-primary btn-sm" @click="resumePractice">继续练习</button>
            <button class="btn btn-outline btn-sm" @click="discardResume">放弃</button>
          </div>
        </div>
        <div class="mode-grid">
          <div class="mode-card" @click="selectMode('exam')">
            <div class="mode-icon">📋</div>
            <div class="mode-name">套卷模式</div>
            <div class="mode-desc">完整年份真题练习，计时模拟考试</div>
          </div>
          <div class="mode-card" @click="selectMode('practice')">
            <div class="mode-icon">📝</div>
            <div class="mode-name">练习模式</div>
            <div class="mode-desc">按科目专项练习，即时查看答案</div>
          </div>
          <div class="mode-card" @click="selectMode('smart')">
            <div class="mode-icon">🎯</div>
            <div class="mode-name">专项练习</div>
            <div class="mode-desc">按知识点专项练习，只刷该知识点题目</div>
          </div>
          <div class="mode-card" @click="selectMode('ai')" style="border-color:rgba(124,58,237,0.3);">
            <div class="mode-icon">✨</div>
            <div class="mode-name">AI 变式题</div>
            <div class="mode-desc">AI 基于真题生成同知识点变式题</div>
          </div>
        </div>
      </template>

      <template v-else-if="phase === 'setup'">
        <div class="page-header">
          <h2>{{ selectedMode === 'exam' ? '套卷模式' : (selectedMode === 'smart' ? '专项练习' : '练习模式') }}</h2>
          <button class="btn btn-outline btn-sm" @click="phase = 'mode_select'">← 返回</button>
        </div>
        <div class="card">
          <template v-if="selectedMode === 'exam'">
            <div class="form-group">
              <label class="form-label">选择年份</label>
              <select class="form-input" v-model="form.year">
                <option value="">-- 选择年份 --</option>
                <option v-for="y in years" :key="y" :value="y">{{ y }}年</option>
              </select>
            </div>
            <div style="margin-bottom:16px;">
              <label style="display:flex;align-items:center;gap:6px;cursor:pointer;font-size:14px;">
                <input type="checkbox" v-model="examMode" /> 🎯 考场模式
              </label>
            </div>
          </template>

          <template v-else-if="selectedMode === 'practice'">
            <div class="form-group">
              <label class="form-label">选择科目</label>
              <select class="form-input" v-model="form.subject">
                <option value="">-- 选择科目 --</option>
                <option value="数据结构">数据结构</option>
                <option value="计算机组成原理">计算机组成原理</option>
                <option value="操作系统">操作系统</option>
                <option value="计算机网络">计算机网络</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">题型</label>
              <div class="tab-bar">
                <button class="tab-btn" :class="{ active: form.type === '单选' }" @click="form.type = '单选'">单选题</button>
                <button class="tab-btn" :class="{ active: form.type === '综合应用' }" @click="form.type = '综合应用'">综合应用题</button>
              </div>
            </div>
          </template>

          <template v-else-if="selectedMode === 'smart'">
            <div class="form-group">
              <label class="form-label">选择科目</label>
              <select class="form-input" v-model="form.subject" @change="loadKnowledgeTags">
                <option value="">-- 选择科目 --</option>
                <option value="数据结构">数据结构</option>
                <option value="计算机组成原理">计算机组成原理</option>
                <option value="操作系统">操作系统</option>
                <option value="计算机网络">计算机网络</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">选择知识点</label>
              <select class="form-input" v-model="form.knowledgeTag">
                <option value="">-- 选择知识点 --</option>
                <option v-for="tag in knowledgeTags" :key="tag" :value="tag">{{ tag }}</option>
              </select>
            </div>
          </template>

          <template v-else-if="selectedMode === 'ai'">
            <div class="form-group">
              <label class="form-label">选择科目</label>
              <select class="form-input" v-model="form.subject" @change="loadKnowledgeTags">
                <option value="">-- 选择科目 --</option>
                <option value="数据结构">数据结构</option>
                <option value="计算机组成原理">计算机组成原理</option>
                <option value="操作系统">操作系统</option>
                <option value="计算机网络">计算机网络</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">选择知识点（可选）</label>
              <select class="form-input" v-model="form.knowledgeTag">
                <option value="">-- 不指定，按科目随机 --</option>
                <option v-for="tag in knowledgeTags" :key="tag" :value="tag">{{ tag }}</option>
              </select>
              <div style="font-size:12px;color:var(--text-secondary);margin-top:6px;">
                AI 会基于同知识点的真题生成变式题，每次生成 1 道。
              </div>
            </div>
          </template>

          <button
            class="btn btn-lg btn-block"
            @click="startSession"
            :disabled="loading || !canStart"
            :style="{ marginTop:'20px', padding:'14px 32px', fontSize:'16px', fontWeight:'600', display:'block', background: canStart ? '#f57c00' : '#ffcc80', color: canStart ? '#fff' : '#666', border:'none', borderRadius:'8px', cursor: canStart ? 'pointer' : 'not-allowed' }"
          >
            {{ loading ? '加载中...' : (!canStart ? getStartButtonText() : '开始刷题') }}
          </button>
        </div>
      </template>

      <template v-else-if="phase === 'exam'">
        <div class="exam-header">
          <button class="btn btn-outline btn-sm" @click="confirmExitExam">← 退出考试</button>
          <div class="exam-timer" :class="{ warning: timerSeconds > 30 * 60 }">⏱️ {{ formatTime(timerSeconds) }}</div>
          <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap;">
            <button class="btn btn-outline btn-sm" @click="exportCurrentPdf">📄 导出PDF</button>
            <button class="btn btn-outline btn-sm" @click="showAnswerCard = !showAnswerCard">📋 答题卡</button>
            <button class="btn btn-primary btn-sm" @click="confirmSubmitExam">✓ 提交试卷</button>
            <button class="btn btn-outline btn-sm" @click="goBack">← 返回</button>
          </div>
        </div>

        <div class="question-item" v-if="curExamQ">
          <div class="question-header">
            <span class="question-badge">{{ curExamQ.year || curExamQ.examYear }}</span>
            <span class="question-badge" style="background:#e8f5e9;color:#2e7d32;">第{{ curExamQ.questionNumber }}题</span>
            <span class="question-badge" style="background:#f57c00;color:#fff;">{{ getScore(curExamQ) }}分</span>
            <span class="question-number" style="margin-left:auto;">{{ examIndex + 1 }}/{{ examQuestions.length }}</span>
          </div>
          <div class="question-content" v-html="formatQuestionContent(curExamQ.content)"></div>
          <div v-if="parsePageImg(curExamQ.content)" style="margin-bottom:14px;">
            <img :src="parsePageImg(curExamQ.content)" style="max-width:100%;border-radius:8px;border:1px solid var(--border);" alt="原题图片" />
          </div>
          <div v-if="getQuestionImages(curExamQ.year || curExamQ.examYear, curExamQ.questionNumber).length > 0" v-for="(img, idx)
          in getQuestionImages(curExamQ.year || curExamQ.examYear, curExamQ.questionNumber)" :key="idx" style="margin-bottom:14px;">
            <img :src="img" style="max-width:100%;border-radius:8px;border:1px solid var(--border);" :alt="'题目图片'" @error="(e) => { e.target.style.display='none'; console.log('加载失败:', img); }" />
          </div>

          <ul v-if="curExamQ.type === '单选'" class="options-list">
            <li v-for="opt in parseOptions(curExamQ.options, curExamQ.questionNumber, curExamQ.year || curExamQ.examYear)" :key="opt.key"
                class="option-item" :class="{ selected: examAnswers[curExamQ.id] === opt.key }" @click="selectExamAnswer(curExamQ, opt.key)">
              <span class="option-key">{{ opt.key }}</span>
              <span>{{ opt.text }}</span>
            </li>
          </ul>

          <div v-if="curExamQ.type === '综合应用'" class="essay-answer-box">
            <div class="essay-answer-title">📜 综合应用题</div>
            <div style="color:var(--text-secondary);font-size:13px;">本题仅展示参考答案：交卷后可见答案与解析，不计入总分。</div>
          </div>
        </div>

        <div class="exam-nav">
          <div class="nav-buttons" style="display:flex;justify-content:space-between;align-items:center;">
            <button class="btn btn-outline" :disabled="examIndex === 0" @click="prevExamQuestion">← 上一题</button>
            <span style="font-size:13px;color:var(--text-secondary);">{{ examAnsweredCount }}/{{ examSingleCount }} 单选已答</span>
            <button v-if="examIndex < examQuestions.length - 1" class="btn btn-outline" @click="nextExamQuestion">下一题 →</button>
            <button v-else class="btn btn-primary" @click="confirmSubmitExam" :disabled="submitting">{{ submitting ? '提交中...' : '提交试卷' }}</button>
          </div>
        </div>

        <div v-if="showAnswerCard" class="answer-card-overlay" @click.self="showAnswerCard = false">
          <div class="answer-card">
            <h3>答题卡</h3>
            <div class="answer-grid">
              <button v-for="(q, i) in examQuestions" :key="q.id" class="answer-card-btn" :class="{ answered: examAnswers[q.id] }" @click="jumpToExamQuestion(i)">{{ i + 1 }}</button>
            </div>
          </div>
        </div>

        <div v-if="showSubmitConfirm" class="answer-card-overlay" @click.self="showSubmitConfirm = false">
          <div class="confirm-modal">
            <h3 style="margin:0 0 12px 0;">确认提交试卷？</h3>
            <div style="color:var(--text-secondary);line-height:1.8;">
              <div>📝 单选题数：<strong style="color:var(--text);">{{ examSingleCount }}</strong> 题</div>
              <div v-if="examQuestions.length - examSingleCount > 0" style="margin-top:4px;">
                📜 另有 {{ examQuestions.length - examSingleCount }} 道综合应用题：交卷后展示参考答案，不计分
              </div>
              <div style="margin-top:6px;">✅ 单选已作答：<strong style="color:var(--success);">{{ examAnsweredCount }}</strong> 题</div>
              <div v-if="examSingleCount - examAnsweredCount > 0" style="color:var(--warning);font-weight:600;">
                ⚠️ 还有 {{ examSingleCount - examAnsweredCount }} 道单选未作答
              </div>
              <div style="margin-top:12px;">提交后将不能修改答案，确定要提交吗？</div>
            </div>
            <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:20px;">
              <button class="btn btn-outline" @click="showSubmitConfirm = false">再检查一下</button>
              <button class="btn btn-primary" @click="submitExam" :disabled="submitting">{{ submitting ? '提交中...' : '确认提交' }}</button>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="phase === 'practice'">
        <div class="practice-header">
          <div class="practice-mode-label">
            <span v-if="selectedMode === 'smart'" class="mode-badge smart-mode">📌 专项练习</span>
            <span v-else-if="selectedMode === 'ai'" class="mode-badge" style="background:rgba(124,58,237,0.8);">✨ AI 变式题</span>
            <span v-else class="mode-badge">📝 练习模式</span>
            <span v-if="selectedMode === 'smart' && form.knowledgeTag" class="knowledge-tag-label">当前知识点：{{ form.knowledgeTag }}</span>
            <span class="knowledge-tag-label">{{ form.subject }} · {{ form.type }}</span>
          </div>
          <div class="practice-header-actions">
            <button class="ph-btn" @click="exportCurrentPdf">📄 导出PDF</button>
            <button class="ph-btn ph-btn-danger" @click="exitPracticeSession">退出练习</button>
            <button class="ph-btn" @click="goBack">← 返回</button>
          </div>
        </div>

        <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin-bottom:20px;">
          <div class="practice-switcher">
            <div class="practice-subject-tabs">
              <button v-for="s in subjects" :key="s" class="practice-subject-tab" :class="{ active: form.subject === s }" @click="switchPracticeSubject(s)">{{ s }}</button>
            </div>
            <div class="practice-type-tabs">
              <button class="practice-type-tab" :class="{ active: form.type === '单选' }" @click="switchPracticeType('单选')">单选题</button>
              <button class="practice-type-tab" :class="{ active: form.type === '综合应用' }" @click="switchPracticeType('综合应用')">综合应用题</button>
            </div>
            <div class="practice-view-tabs">
              <button class="tab-btn" :class="{ active: practiceMode === 'single' }" @click="switchPracticeView('single')">单题模式</button>
              <button class="tab-btn" :class="{ active: practiceMode === 'list' }" @click="switchPracticeView('list')">列表模式</button>
            </div>
          </div>
          <div v-if="practiceMode === 'single' && practiceQuestions.length" style="margin-left:auto;display:flex;gap:10px;">
            <button class="btn btn-outline" :disabled="practiceIndex === 0" @click="prevPracticeQuestion">← 上一题</button>
            <button class="btn btn-primary" :disabled="practiceIndex >= practiceQuestions.length - 1" @click="nextPracticeQuestion">下一题 →</button>
          </div>
        </div>

        <div v-if="practiceLoading" class="practice-loading">加载题目中...</div>

        <template v-else-if="practiceMode === 'single' && practiceQuestions.length">
          <div class="question-item" v-if="curPracticeQ">
            <div class="question-header">
              <span class="question-badge">{{ curPracticeQ.year || curPracticeQ.examYear }}</span>
              <span class="question-badge" style="background:#e8f5e9;color:#2e7d32;">第{{ curPracticeQ.questionNumber }}题</span>
              <span class="question-badge" style="background:#f57c00;color:#fff;">{{ getScore(curPracticeQ) }}分</span>
              <span v-if="curPracticeQ.knowledgeTag" class="question-badge" style="background:#e3f2fd;color:#1976d2;">{{ curPracticeQ.knowledgeTag }}</span>
              <span class="question-number" style="margin-left:auto;">{{ practiceIndex + 1 }}/{{ practiceQuestions.length }}</span>
            </div>
            <div class="question-content" v-html="formatQuestionContent(curPracticeQ.content) || '暂无题目内容'"></div>
            <div v-if="parsePageImg(curPracticeQ.content)" style="margin-bottom:14px;">
              <img :src="parsePageImg(curPracticeQ.content)" style="max-width:100%;border-radius:8px;border:1px solid var(--border);" alt="原题图片" />
            </div>
            <div v-for="(img, idx) in getQuestionImages(curPracticeQ.year || curPracticeQ.examYear, curPracticeQ.questionNumber)" :key="idx" style="margin-bottom:14px;">
              <img :src="img" style="max-width:100%;border-radius:8px;border:1px solid var(--border);" :alt="'题目图片'" @error="(e) => { e.target.style.display='none'; console.log('加载失败:', img); }" />
            </div>

            <div v-if="curPracticeQ.type === '单选' && parseOptions(curPracticeQ.options, curPracticeQ.questionNumber, curPracticeQ.year || curPracticeQ.examYear).some(o => o.image)" class="options-images-container">
              <div v-for="(opt, idx) in parseOptions(curPracticeQ.options, curPracticeQ.questionNumber, curPracticeQ.year || curPracticeQ.examYear)" :key="idx" class="option-image-item" v-if="opt && opt.image">
                <img :src="opt.image" style="max-width:100%;border-radius:4px;" :alt="'选项' + opt.key + '图片'" />
                <span class="option-label">{{ opt.key }}</span>
              </div>
            </div>

            <div v-if="curPracticeQ.type === '单选'">
              <ul class="options-list" v-if="parseOptions(curPracticeQ.options, curPracticeQ.questionNumber, curPracticeQ.year || curPracticeQ.examYear).length">
                <li v-for="opt in parseOptions(curPracticeQ.options, curPracticeQ.questionNumber, curPracticeQ.year || curPracticeQ.examYear)" :key="opt.key" class="option-item" :class="getOptionClass(opt.key)" @click="selectPracticeOption(opt.key)">
                  <span class="option-key">{{ opt.key }}</span>
                  <span>{{ opt.text }}</span>
                </li>
              </ul>
              <div v-else style="color:#999;font-style:italic;padding:12px;border:1px dashed #ddd;border-radius:8px;">暂无选项数据</div>

              <div class="analysis-box" :class="practiceRevealed ? '' : 'an-locked'">
                <div v-if="practiceUserAnswer || practiceTextAnswer" :style="{ color: practiceIsCorrect ? 'var(--success)' : 'var(--danger)', fontWeight: '600', fontSize: '14px' }">{{ practiceIsCorrect ? '✅ 回答正确！' : '❌ 回答错误' }}</div>
                <div v-else style="color:#1565c0;font-weight:600;font-size:14px;">💡 参考答案</div>
                <span class="an-item"><strong>正确答案：</strong>{{ curPracticeQ.answer || '暂无答案' }}</span>
                <span v-if="practiceUserAnswer || practiceTextAnswer" class="an-item"><strong>你的选择：</strong><span :style="{ color: practiceIsCorrect ? 'var(--success)' : 'var(--danger)' }">{{ practiceUserAnswer || '未作答' }}</span></span>
                <span v-if="curPracticeQ.analysis" class="an-item"><strong>解析：</strong>{{ formatAnalysis(curPracticeQ.analysis) }}</span>
                <span v-else class="an-item" style="color:var(--text-secondary);">暂无解析</span>
                <button v-if="!practiceRevealed" class="an-lock-btn" @click="revealPracticeAnswer">🔒 查看答案与解析</button>
              </div>
            </div>

            <div v-else-if="curPracticeQ.type === '综合应用'" class="essay-answer-box" style="margin-bottom:8px;">
              <div class="essay-answer-title">📜 综合应用题</div>
              <div style="color:var(--text-secondary);font-size:13px;margin-bottom:6px;">本题仅展示参考答案，不参与作答与计分。</div>
              <div class="analysis-box">
                <div style="color:#1565c0;font-weight:600;font-size:14px;">💡 参考答案</div>
                <span class="an-item"><strong>答案：</strong>{{ curPracticeQ.answer || '暂无答案' }}</span>
                <span v-if="curPracticeQ.analysis" class="an-item"><strong>解析：</strong>{{ formatAnalysis(curPracticeQ.analysis) }}</span>
                <span v-else class="an-item" style="color:var(--text-secondary);">暂无解析</span>
              </div>
            </div>
          </div>

          <!-- 单题模式底部常驻翻页条：长题目滚动后也能随时切题 -->
          <div class="question-pager">
            <button class="pager-btn" :disabled="practiceIndex === 0" @click="prevPracticeQuestion">← 上一题</button>
            <div class="pager-progress">
              <span class="pager-index">{{ practiceIndex + 1 }} / {{ practiceQuestions.length }}</span>
              <input
                class="pager-slider"
                type="range"
                min="1"
                :max="practiceQuestions.length"
                :value="practiceIndex + 1"
                @input="jumpToPracticeQuestion($event)"
              />
            </div>
            <button class="pager-btn pager-btn-primary" :disabled="practiceIndex >= practiceQuestions.length - 1" @click="nextPracticeQuestion">下一题 →</button>
          </div>
        </template>

        <template v-else-if="practiceMode === 'list' && practiceQuestions.length">
          <div class="practice-list-container">
            <div v-if="selectedMode === 'ai'" class="ai-gen-bar">
              <span style="font-size:13px;color:var(--text-secondary);">✨ AI 变式题 · {{ practiceQuestions.length }} 题</span>
              <button class="btn btn-primary btn-sm" @click="generateMoreAiQuestion" :disabled="practiceLoading" style="background:#7c3aed;">
                {{ practiceLoading ? '生成中…' : '+ 再生成一道' }}
              </button>
            </div>
            <div v-for="(q, index) in practiceQuestions" :key="q.id" class="question-item list-mode-item">
              <div class="question-header">
                <span v-if="q.year || q.examYear" class="question-badge">{{ q.year || q.examYear }}</span>
                <span v-else class="question-badge" style="background:rgba(124,58,237,0.12);color:#7c3aed;">AI生成</span>
                <span v-if="q.questionNumber" class="question-badge" style="background:#e8f5e9;color:#2e7d32;">第{{ q.questionNumber }}题</span>
                <span v-if="q.source !== 'ai'" class="question-badge" style="background:#f57c00;color:#fff;">{{ getScore(q) }}分</span>
                <span v-if="q.knowledgeTag" class="question-badge" style="background:#e3f2fd;color:#1976d2;">{{ q.knowledgeTag }}</span>
                <span class="question-number" style="margin-left:auto;">{{ index + 1 }}/{{ practiceQuestions.length }}</span>
              </div>
              <div class="question-content" v-html="formatQuestionContent(q.content) || '暂无题目内容'"></div>
              <div v-if="parsePageImg(q.content)" style="margin-bottom:14px;">
                <img :src="parsePageImg(q.content)" style="max-width:100%;border-radius:8px;border:1px solid var(--border);" alt="原题图片" />
              </div>
              <div v-for="(img, idx) in getQuestionImages(q.year || q.examYear, q.questionNumber)" :key="idx" style="margin-bottom:14px;">
                <img :src="img" style="max-width:100%;border-radius:8px;border:1px solid var(--border);" :alt="'题目图片'" @error="(e) => e.target.style.display='none'" />
              </div>

              <div v-if="q.type === '单选' && parseOptions(q.options, q.questionNumber, q.year || q.examYear).some(o => o.image)" class="options-images-container">
                <div v-for="(opt, idx) in parseOptions(q.options, q.questionNumber, q.year || q.examYear)" :key="idx" class="option-image-item" v-if="opt && opt.image">
                  <img :src="opt.image" style="max-width:100%;border-radius:4px;" :alt="'选项' + opt.key + '图片'" />
                  <span class="option-label">{{ opt.key }}</span>
                </div>
              </div>

              <div v-if="q.type === '单选'">
                <ul class="options-list" v-if="parseOptions(q.options, q.questionNumber, q.year || q.examYear).length">
                  <li v-for="opt in parseOptions(q.options, q.questionNumber, q.year || q.examYear)" :key="opt.key" class="option-item" :class="getListItemOptionClass(q, opt.key)" @click="onListItemOptionClick(q, opt.key)">
                    <span class="option-key">{{ opt.key }}</span>
                    <span>{{ opt.text }}</span>
                  </li>
                </ul>
                <div v-else style="color:#999;font-style:italic;padding:12px;border:1px dashed #ddd;border-radius:8px;">暂无选项数据</div>

                <div class="analysis-box" :class="q._revealed ? '' : 'an-locked'">
                  <div v-if="q._userAnswer || q._textAnswer" :style="{ color: q._correct ? 'var(--success)' : 'var(--danger)', fontWeight: '600', fontSize: '14px' }">{{ q._correct ? '✅ 回答正确！' : '❌ 回答错误' }}</div>
                  <div v-else style="color:#1565c0;font-weight:600;font-size:14px;">💡 参考答案</div>
                  <span class="an-item"><strong>正确答案：</strong>{{ q.answer || '暂无答案' }}</span>
                  <span v-if="q._userAnswer || q._textAnswer" class="an-item"><strong>你的选择：</strong><span :style="{ color: q._correct ? 'var(--success)' : 'var(--danger)' }">{{ q._userAnswer || '未作答' }}</span></span>
                  <span v-if="q.analysis" class="an-item"><strong>解析：</strong>{{ formatAnalysis(q.analysis) }}</span>
                  <span v-else class="an-item" style="color:var(--text-secondary);">暂无解析</span>
                  <button v-if="!q._revealed" class="an-lock-btn" @click="revealListItemAnswer(q)">🔒 查看答案与解析</button>
                </div>
              </div>

              <div v-else-if="q.type === '综合应用'" style="margin-bottom:8px;">
                <div class="essay-answer-box">
                  <div class="essay-answer-title">📜 综合应用题</div>
                  <div style="color:var(--text-secondary);font-size:13px;margin-bottom:6px;">本题仅展示参考答案，不参与作答与计分。</div>
                  <div class="analysis-box">
                    <div style="color:#1565c0;font-weight:600;font-size:14px;">💡 参考答案</div>
                    <span class="an-item"><strong>答案：</strong>{{ q.answer || '暂无答案' }}</span>
                    <span v-if="q.analysis" class="an-item"><strong>解析：</strong>{{ formatAnalysis(q.analysis) }}</span>
                    <span v-else class="an-item" style="color:var(--text-secondary);">暂无解析</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <div v-else class="practice-empty">当前筛选条件下无题目</div>

        <div v-if="showCompleteModal" class="complete-modal-overlay" @click.self="showCompleteModal = false">
          <div class="complete-modal">
            <div class="complete-modal-icon">🎉</div>
            <h3 class="complete-modal-title">太棒了！</h3>
            <p class="complete-modal-desc">你已完成「{{ completeModalData.tag || knowledgeTagFilter }}」的所有题目</p>
            <div class="complete-modal-stats">
              <div class="stat-item">
                <span class="stat-num">{{ completeModalData.total }}</span>
                <span class="stat-label">总题数</span>
              </div>
              <div class="stat-item">
                <span class="stat-num correct">{{ completeModalData.correct }}</span>
                <span class="stat-label">正确</span>
              </div>
              <div class="stat-item">
                <span class="stat-num wrong">{{ completeModalData.wrong }}</span>
                <span class="stat-label">错误</span>
              </div>
            </div>
            <p class="complete-modal-message">{{ getEncouragementMessage(completeModalData.correct, completeModalData.total) }}</p>
            <div class="complete-modal-actions">
              <button class="btn btn-primary" @click="resetToMode">继续练习</button>
              <button class="btn btn-outline" @click="goToOverview">返回知识总览</button>
            </div>
          </div>
        </div>
      </template>

      <template v-else-if="phase === 'result'">
        <div class="result-page">
          <div class="result-header" :class="{ excellent: result.accuracy >= 90, good: result.accuracy >= 70 && result.accuracy < 90, pass: result.accuracy >= 50 && result.accuracy < 70, low: result.accuracy < 50 }">
            <div class="result-icon">
              <span style="font-size:56px;">{{ result.accuracy >= 90 ? '🏆' : result.accuracy >= 70 ? '🎉' : result.accuracy >= 50 ? '💪' : '📚' }}</span>
            </div>
            <h2>{{ result.accuracy >= 90 ? '太棒了！' : result.accuracy >= 70 ? '做得不错！' : result.accuracy >= 50 ? '考试完成！' : '继续加油！' }}</h2>
            <p style="color:rgba(255,255,255,0.9);margin:8px 0 4px 0;">用时 {{ formatTime(result.duration) }}</p>
            <div class="result-message" :class="{ 'low-score': result.accuracy < 50 }">
              {{ getExamEncouragement(result.score, result.totalScore, result.accuracy) }}
            </div>
          </div>

          <div class="result-score-display">
            <div class="score-big">
              <span class="score-num">{{ result.score }}</span>
              <span class="score-sep">/</span>
              <span class="score-total">{{ result.totalScore }}</span>
            </div>
            <div class="score-label">总分</div>
          </div>

          <div class="result-stats">
            <div class="result-stat">
              <span class="result-stat-num correct">{{ result.correct }}</span>
              <span class="result-stat-label">✓ 正确</span>
            </div>
            <div class="result-stat">
              <span class="result-stat-num wrong">{{ result.wrong }}</span>
              <span class="result-stat-label">✗ 错误</span>
            </div>
            <div class="result-stat">
              <span class="result-stat-num" style="color:#f57c00;">{{ result.unanswered }}</span>
              <span class="result-stat-label">○ 未答</span>
            </div>
            <div class="result-stat">
              <span class="result-stat-num">{{ result.accuracy }}%</span>
              <span class="result-stat-label">正确率</span>
            </div>
          </div>

          <div v-if="result.subjectBreakdown && result.subjectBreakdown.length > 0" class="result-analysis">
            <h3 style="margin:0 0 12px 0;font-size:17px;color:var(--text);">📊 分科分析</h3>
            <div v-for="sb in result.subjectBreakdown" :key="sb.subject" class="subject-breakdown">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                <span style="font-weight:600;">{{ sb.subject }}</span>
                <span style="color:var(--text-secondary);font-size:13px;">{{ sb.score }} / {{ sb.totalScore }} 分 · 正确率 {{ sb.accuracy }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: sb.accuracy + '%', background: sb.accuracy >= 70 ? 'var(--success)' : sb.accuracy >= 50 ? '#f57c00' : 'var(--danger)' }"></div>
              </div>
              <div style="font-size:12px;color:var(--text-secondary);margin-top:4px;">正确 {{ sb.correct }} 题 · 错误 {{ sb.wrong }} 题</div>
            </div>
          </div>

          <div v-if="result.knowledgeBreakdown && result.knowledgeBreakdown.length > 0" class="result-analysis">
            <h3 style="margin:0 0 12px 0;font-size:17px;color:var(--text);">🎯 知识点分析（薄弱点优先）</h3>
            <div v-for="kb in result.knowledgeBreakdown" :key="kb.tag" class="subject-breakdown">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;gap:8px;">
                <span style="font-weight:600;">{{ kb.tag }}</span>
                <span style="color:var(--text-secondary);font-size:12px;flex-shrink:0;">{{ kb.score }} / {{ kb.totalScore }} 分 · 正确率 {{ kb.accuracy }}%</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: kb.accuracy + '%', background: kb.accuracy >= 70 ? 'var(--success)' : kb.accuracy >= 50 ? '#f57c00' : 'var(--danger)' }"></div>
              </div>
              <div style="display:flex;justify-content:space-between;align-items:center;margin-top:4px;font-size:12px;color:var(--text-secondary);">
                <span>正确 {{ kb.correct }} 题 · 错误 {{ kb.wrong }} 题</span>
                <button class="btn btn-sm" style="background:#059669;color:#fff;border:none;" @click="goKnowledgePractice(kb)">📌 生成该知识点练习</button>
              </div>
            </div>
          </div>

          <div v-if="result.details && result.details.filter(d => !d.correct && !d.essay).length > 0" class="result-analysis">
            <h3 style="margin:0 0 12px 0;font-size:17px;color:var(--text);">📝 错题回顾</h3>
            <div v-for="(detail, index) in result.details.filter(d => !d.correct && !d.essay)" :key="index" class="result-detail wrong-detail">
              <div style="display:flex;align-items:flex-start;gap:10px;">
                <span class="detail-num" style="background:var(--danger);color:#fff;flex-shrink:0;">{{ detail.questionNumber }}</span>
                <div style="flex:1;min-width:0;">
                  <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                    <span style="font-size:12px;color:var(--text-secondary);">{{ detail.subject }} · {{ detail.type }}</span>
                  </div>
                  <div style="font-size:14px;line-height:1.6;margin-bottom:8px;" v-html="detail.content"></div>
                  <div style="font-size:13px;line-height:1.8;">
                    <div>你的答案：<span style="color:var(--danger);font-weight:600;">{{ detail.userAnswer || '未作答' }}</span></div>
                    <div>正确答案：<span style="color:var(--success);font-weight:600;">{{ detail.answer }}</span></div>
                    <div v-if="detail.analysis" style="margin-top:6px;color:var(--text-secondary);border-left:3px solid var(--border);padding-left:10px;">
                      💡 {{ detail.analysis }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="result-analysis">
            <h3 style="margin:0 0 12px 0;font-size:17px;color:var(--text);">📋 完整答题记录</h3>
            <div v-for="(detail, index) in result.details" :key="index" class="result-detail" :class="{ correct: detail.correct, wrong: !detail.correct }">
              <span class="detail-num" :style="{ background: detail.correct ? 'var(--success)' : (detail.essay ? 'var(--text-secondary)' : 'var(--danger)'), color:'#fff' }">{{ detail.questionNumber }}</span>
              <span class="detail-info">
                <span>{{ detail.subject }}</span>
                <span v-if="detail.type === '综合应用'" style="color:var(--text-secondary);font-weight:600;">不计分</span>
                <span :style="{ color: detail.essay ? 'var(--text-secondary)' : (detail.correct ? 'var(--success)' : 'var(--danger)'), fontWeight:600 }">{{ detail.essay ? '—' : (detail.correct ? '✓' : '✗') }}</span>
              </span>
              <span class="detail-answer">
                <span v-if="detail.essay">参考答案：<span style="color:var(--success);">查看下方解析</span></span>
                <span v-else>{{ detail.userAnswer || '未作答' }}</span>
                <span v-if="!detail.correct && !detail.essay" style="color:var(--success);margin-left:6px;">→ {{ detail.answer }}</span>
              </span>
            </div>
          </div>

          <div style="display:flex;gap:12px;justify-content:center;margin-top:24px;flex-wrap:wrap;">
            <button class="btn btn-outline" @click="exportPracticePdf">📄 导出PDF</button>
            <button class="btn btn-primary" @click="resetToMode">重新选择模式</button>
            <button class="btn btn-outline" @click="$router.push('/')">返回首页</button>
          </div>
        </div>
      </template>
    </main>

    <!-- AI 讲解抽屉 -->
    <AiChatPanel v-model="aiPanel.visible" :question="aiPanel.question" :user-answer="aiPanel.userAnswer" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '../api'
import AiChatPanel from '../components/ai/AiChatPanel.vue'
import { useAiContextStore } from '../stores/aiContext'
import { formatQuestionContent, formatAnalysis } from '../utils/questionContent'
import { exportPdf, questionBlock } from '../utils/pdfExport'

const router = useRouter()
const route = useRoute()

// AI 讲解抽屉
const aiCtx = useAiContextStore()
const aiPanel = ref({ visible: false, question: null, userAnswer: '' })
function openAiPanel(q, userAnswer) {
  if (!q || !q.id) return
  const ctx = { id: q.id, year: q.year || q.examYear, subject: q.subject, knowledgeTag: q.knowledgeTag }
  aiCtx.setQuestion(ctx)
  aiPanel.value = { visible: true, question: ctx, userAnswer: userAnswer || '' }
}

function openAiPanelQ(detail) {
  if (!detail || !detail.questionId) return
  const q = { id: detail.questionId, year: detail.year, subject: detail.subject, knowledgeTag: detail.knowledgeTag }
  aiCtx.setQuestion(q)
  aiPanel.value = { visible: true, question: q, userAnswer: detail.userAnswer || '' }
}

function goKnowledgePractice(kb) {
  if (!kb || !kb.tag) return
  router.push({ path: '/practice', query: { subject: kb.subject || '', knowledgeTag: kb.tag } })
}

function exportPracticePdf() {
  const r = result.value
  const items = r.details || []
  const byId = {}
  for (const q of examQuestions.value) byId[q.id] = q
  const blocks = items.map(d => {
    const src = d.questionId ? byId[d.questionId] : null
    return questionBlock({
      year: d.year,
      questionNumber: d.questionNumber,
      type: d.type,
      knowledgeTag: d.knowledgeTag,
      content: d.content,
      options: src && src.options ? parseOptions(src.options, d.questionNumber, d.year) : [],
      userAnswer: d.userAnswer,
      answer: d.answer,
      essay: d.essay,
      correct: d.essay ? undefined : d.correct,
      analysis: formatAnalysis(d.analysis)
    })
  })
  const modeLabel = selectedMode.value === 'exam' ? '套卷模式' : (selectedMode.value === 'smart' ? '专项练习' : '练习模式')
  const sub = `模式：${modeLabel} · ${form.value.subject || ''} · ${form.value.type || ''} · 得分 ${r.score} / ${r.totalScore} · 正确率 ${r.accuracy}% · 用时 ${formatTime(r.duration)}`
  if (!exportPdf({ title: '408真题练习结果', subtitle: sub, blocks })) {
    alert('请允许浏览器打开新窗口后重试（用于生成 PDF）')
  }
}

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/')
}

function exitPracticeSession() {
  if (!confirm('确定要退出本次练习吗？当前进度将不会保存。')) return
  clearPracticeResume()
  stopTimer()
  resetToMode()
}

function exportCurrentPdf() {
  let qs, label, sub
  if (selectedMode.value === 'exam') {
    qs = examQuestions.value
    label = '套卷真题'
    sub = `${form.value.year || ''}年 · 共 ${qs.length} 题`
  } else {
    qs = practiceQuestions.value
    label = selectedMode.value === 'smart' ? '专项练习' : (selectedMode.value === 'ai' ? 'AI 变式题' : '练习')
    sub = `${form.value.subject || ''} · ${form.value.type || ''} · ${qs.length} 题`
  }
  if (!qs.length) { alert('当前没有可导出的题目'); return }
  const items = qs.map(q => {
    const isEssay = q.type === '综合应用'
    const ua = selectedMode.value === 'exam' ? (examAnswers.value[q.id] || '') : (q._userAnswer || q._textAnswer || '')
    return questionBlock({
      year: q.year || q.examYear,
      questionNumber: q.questionNumber,
      type: q.type,
      knowledgeTag: q.knowledgeTag,
      content: formatQuestionContent(q.content),
      options: q.options ? parseOptions(q.options, q.questionNumber, q.year || q.examYear) : [],
      userAnswer: ua,
      answer: q.answer,
      essay: isEssay,
      correct: isEssay ? undefined : (ua === q.answer),
      analysis: formatAnalysis(q.analysis)
    })
  })
  if (!exportPdf({ title: '408 ' + label, subtitle: sub, blocks: items })) {
    alert('请允许浏览器打开新窗口后重试（用于生成 PDF）')
  }
}

const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
const phase = ref('mode_select')
const selectedMode = ref('')
const loading = ref(false)
const submitting = ref(false)

const years = ref([])
const subjects = ['数据结构', '计算机组成原理', '操作系统', '计算机网络']
const form = ref({ year: '', subject: '数据结构', type: '单选', knowledgeTag: '' })
const examMode = ref(false)
const knowledgeTags = ref([])

const practiceMode = ref('single')
const knowledgeTagFilter = ref('')

const result = ref({ total: 0, correct: 0, wrong: 0, accuracy: 0, score: 0, duration: 0, details: [] })

const examQuestions = ref([])
const examAnswers = ref({})
const examTextAnswers = ref({})
const examIndex = ref(0)
const timerSeconds = ref(0)
const showAnswerCard = ref(false)
let timer = null

const showSubmitConfirm = ref(false)
const examResult = ref({ score: 0, totalScore: 0, correct: 0, wrong: 0, unanswered: 0, accuracy: 0, duration: 0, details: [], subjectBreakdown: [] })

const practiceQuestions = ref([])
const practiceIndex = ref(0)
const practiceRevealed = ref(false)
const practiceUserAnswer = ref('')
const practiceTextAnswer = ref('')
const practiceIsCorrect = ref(false)
const practiceLoading = ref(false)

const showCompleteModal = ref(false)
const completeModalData = ref({ total: 0, correct: 0, wrong: 0, tag: '' })

// 专项练习中断恢复（localStorage 按用户隔离）
const resumeInfo = ref(null)
function resumeKey() {
  const uid = user.value?.id
  return uid ? `practice_resume_v1_${uid}` : ''
}
function readPracticeResume() {
  try {
    const key = resumeKey()
    if (!key) return
    const raw = localStorage.getItem(key)
    if (!raw) return
    const saved = JSON.parse(raw)
    if (!saved || !saved.mode) return
    resumeInfo.value = {
      mode: saved.mode,
      subject: saved.subject,
      type: saved.type || '单选',
      knowledgeTag: saved.knowledgeTag || '',
      practiceMode: saved.practiceMode || 'list',
      total: saved.total || 0,
      answered: saved.answered || 0,
      savedAt: saved.savedAt
    }
  } catch (e) { }
}
function savePracticeResume() {
  if (selectedMode.value !== 'practice' && selectedMode.value !== 'smart') return
  if (!user.value?.id) return
  const answers = {}
  let answeredCount = 0
  for (const q of practiceQuestions.value) {
    if (q._answered) answeredCount++
    if (q._userAnswer || q._textAnswer || q._revealed) {
      answers[q.id] = {
        userAnswer: q._userAnswer || '',
        textAnswer: q._textAnswer || '',
        revealed: !!q._revealed,
        answered: !!q._answered
      }
    }
  }
  const payload = {
    mode: selectedMode.value,
    subject: form.value.subject,
    type: form.value.type,
    knowledgeTag: form.value.knowledgeTag || '',
    practiceMode: practiceMode.value,
    index: practiceIndex.value,
    total: practiceQuestions.value.length,
    answered: answeredCount,
    savedAt: new Date().toISOString(),
    answers
  }
  try {
    localStorage.setItem(resumeKey(), JSON.stringify(payload))
  } catch (e) {
    console.error('保存练习进度失败:', e)
  }
}
function clearPracticeResume() {
  try {
    const key = resumeKey()
    if (key) localStorage.removeItem(key)
  } catch (e) { }
  resumeInfo.value = null
}
function rehydratePractice(saved) {
  if (!saved || !saved.answers) return
  for (const q of practiceQuestions.value) {
    const a = saved.answers[q.id]
    if (!a) continue
    q._userAnswer = a.userAnswer || ''
    q._textAnswer = a.textAnswer || ''
    q._revealed = !!a.revealed
    q._answered = !!a.answered
    q._correct = !!(a.answered && a.userAnswer && q.answer && a.userAnswer === q.answer)
  }
  practiceIndex.value = Math.min(saved.index || 0, Math.max(0, practiceQuestions.value.length - 1))
  loadCurrentPracticeQ()
  readPracticeResume()
}
async function resumePractice() {
  const meta = resumeInfo.value
  if (!meta) return
  resumeInfo.value = null
  form.value.subject = meta.subject
  form.value.type = meta.type || '单选'
  form.value.knowledgeTag = meta.knowledgeTag || ''
  selectedMode.value = meta.mode || 'smart'
  practiceMode.value = meta.practiceMode || 'list'
  phase.value = 'practice'
  let saved = null
  try {
    const key = resumeKey()
    const raw = key ? localStorage.getItem(key) : null
    saved = raw ? JSON.parse(raw) : null
  } catch (e) { }
  practiceLoading.value = true
  try {
    if (meta.mode === 'practice') {
      await loadPracticeQuestions(saved)
    } else {
      await loadSmartQuestions(saved)
    }
  } catch (e) { }
  practiceLoading.value = false
}

const canStart = computed(() => {
  if (selectedMode.value === 'exam') return !!form.value.year
  if (selectedMode.value === 'practice') return !!form.value.subject
  if (selectedMode.value === 'smart') return !!form.value.subject && !!form.value.knowledgeTag
  if (selectedMode.value === 'ai') return !!form.value.subject
  return false
})

function getStartButtonText() {
  if (selectedMode.value === 'exam') return '请选择年份'
  if (selectedMode.value === 'practice') return '请选择科目'
  if (selectedMode.value === 'smart') {
    if (!form.value.subject) return '请选择科目'
    return '请选择知识点'
  }
  if (selectedMode.value === 'ai') {
    if (!form.value.subject) return '请选择科目'
    return '生成变式题'
  }
  return '请选择'
}

async function loadKnowledgeTags() {
  if (!form.value.subject) {
    knowledgeTags.value = []
    return
  }
  try {
    const r = await api.getKnowledgeTags(form.value.subject)
    if (r.data.code === 200) {
      knowledgeTags.value = r.data.data || []
    }
  } catch (e) {
    console.error('加载知识点失败', e)
    knowledgeTags.value = []
  }
}

const curExamQ = computed(() => examQuestions.value[examIndex.value] || null)
const curPracticeQ = computed(() => practiceQuestions.value[practiceIndex.value] || null)

const examAnsweredCount = computed(() => {
  return examQuestions.value.filter(q => q.type === '单选' && examAnswers.value[q.id]).length
})

const examSingleCount = computed(() => examQuestions.value.filter(q => q.type === '单选').length)

function toggleTheme() {
  document.body.classList.toggle('dark-mode')
}

async function logout() {
  try { await api.logout() } catch (e) { }
  localStorage.removeItem('user')
  router.push('/login')
}

function selectMode(mode) {
  selectedMode.value = mode
  phase.value = 'setup'
  if (mode === 'exam') fetchYears()
  if (mode === 'smart') loadKnowledgeTags()
}

async function fetchYears() {
  try {
    const r = await api.getYears()
    if (r.data.code === 200) {
      years.value = r.data.data || []
    }
  } catch (e) { }
}
async function startSession() {
  loading.value = true
  try {
    if (selectedMode.value === 'exam') {
      const r = await api.listQuestions({ year: form.value.year })
      if (r.data.code === 200) {
        const data = r.data.data
        examQuestions.value = (Array.isArray(data) ? data : data?.records || []).map(q => ({ ...q }))
        examAnswers.value = {}
        examTextAnswers.value = {}
        examIndex.value = 0
        timerSeconds.value = 0
        showAnswerCard.value = false
        phase.value = 'exam'
        startTimer()
        if (examMode.value) {
          try { document.documentElement.requestFullscreen() } catch (e) { }
        }
      }
    } else if (selectedMode.value === 'practice') {
      await loadPracticeQuestions()
      phase.value = 'practice'
    } else if (selectedMode.value === 'smart') {
      await loadSmartQuestions()
      phase.value = 'practice'
      practiceMode.value = 'list'
    } else if (selectedMode.value === 'ai') {
      await generateAiQuestion()
    }
  } catch (e) { }
  loading.value = false
}

async function loadPracticeQuestions(saved) {
  practiceLoading.value = true
  try {
    const params = { subject: form.value.subject, type: form.value.type }
    const tag = knowledgeTagFilter.value || form.value.knowledgeTag
    if (tag) {
      params.knowledgeTag = tag
    }
    const r = await api.listQuestions(params)
    if (r.data.code === 200) {
      const data = r.data.data
      practiceQuestions.value = (Array.isArray(data) ? data : data?.records || []).map(q => ({ ...q, _answered: false, _correct: false }))
    } else {
      practiceQuestions.value = []
    }
    practiceIndex.value = 0
    practiceRevealed.value = false
    practiceUserAnswer.value = ''
    practiceTextAnswer.value = ''
    practiceIsCorrect.value = false
    rehydratePractice(saved)
    savePracticeResume()
  } catch (e) {
    practiceQuestions.value = []
  }
  practiceLoading.value = false
}

async function loadSmartQuestions(saved) {
  practiceLoading.value = true
  try {
    const params = { subject: form.value.subject, knowledgeTag: form.value.knowledgeTag }
    const r = await api.listQuestions(params)
    if (r.data.code === 200) {
      const data = r.data.data
      const questions = (Array.isArray(data) ? data : data?.records || []).map(q => ({ ...q, _answered: false, _correct: false }))
      practiceQuestions.value = questions.sort((a, b) => {
        const tagA = a.knowledgeTag || ''
        const tagB = b.knowledgeTag || ''
        if (tagA !== tagB) return tagA.localeCompare(tagB)
        return (a.questionNumber || 0) - (b.questionNumber || 0)
      })
    } else {
      practiceQuestions.value = []
    }
    practiceIndex.value = 0
    practiceRevealed.value = false
    practiceUserAnswer.value = ''
    practiceTextAnswer.value = ''
    practiceIsCorrect.value = false
    rehydratePractice(saved)
    savePracticeResume()
  } catch (e) { practiceQuestions.value = [] }
  practiceLoading.value = false
}

// AI 变式题生成
async function generateAiQuestion() {
  practiceLoading.value = true
  try {
    const data = { subject: form.value.subject }
    if (form.value.knowledgeTag) data.knowledgeTag = form.value.knowledgeTag
    const r = await api.generateVariant(data)
    if (r.data.code === 200 && r.data.data) {
      const q = { ...r.data.data, _answered: false, _correct: false, _revealed: false, _userAnswer: '', _textAnswer: '' }
      practiceQuestions.value = [q]
      practiceIndex.value = 0
      practiceRevealed.value = false
      practiceUserAnswer.value = ''
      practiceTextAnswer.value = ''
      practiceIsCorrect.value = false
      practiceMode.value = 'list'
      phase.value = 'practice'
    } else {
      alert(r.data.message || '生成失败，请稍后重试')
    }
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '网络异常'
    alert('生成变式题失败：' + msg)
  }
  practiceLoading.value = false
}

async function generateMoreAiQuestion() {
  practiceLoading.value = true
  try {
    const data = { subject: form.value.subject }
    if (form.value.knowledgeTag) data.knowledgeTag = form.value.knowledgeTag
    const r = await api.generateVariant(data)
    if (r.data.code === 200 && r.data.data) {
      const q = { ...r.data.data, _answered: false, _correct: false, _revealed: false, _userAnswer: '', _textAnswer: '' }
      practiceQuestions.value.push(q)
    } else {
      alert(r.data.message || '生成失败')
    }
  } catch (e) {
    alert('生成失败：' + (e.response?.data?.message || e.message))
  }
  practiceLoading.value = false
}

function startTimer() {
  if (timer) clearInterval(timer)
  timer = setInterval(() => {
    timerSeconds.value++
  }, 1000)
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function formatTime(seconds) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) {
    return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }
  return `${m}:${s.toString().padStart(2, '0')}`
}

function selectExamAnswer(q, key) {
  examAnswers.value[q.id] = key
}

function prevExamQuestion() {
  if (examIndex.value > 0) examIndex.value--
}

function nextExamQuestion() {
  if (examIndex.value < examQuestions.value.length - 1) examIndex.value++
}

function jumpToExamQuestion(index) {
  examIndex.value = index
  showAnswerCard.value = false
}

async function confirmExitExam() {
  if (confirm('确定要退出考试吗？当前答题进度将不会保存。')) {
    stopTimer()
    phase.value = 'mode_select'
    showSubmitConfirm.value = false
    if (document.fullscreenElement) {
      try { document.exitFullscreen() } catch (e) { }
    }
  }
}

function confirmSubmitExam() {
  showSubmitConfirm.value = true
}

async function submitExam() {
  if (submitting.value) return
  submitting.value = true
  stopTimer()
  showSubmitConfirm.value = false
  try {
    // 综合应用题不可作答，只提交单选题
    const answers = []
    for (const q of examQuestions.value) {
      if (q.type !== '单选') continue
      answers.push({ questionId: q.id, userAnswer: examAnswers.value[q.id] || '' })
    }

    const response = await api.submitAnswers({
      mode: 'year',
      year: form.value.year,
      answers,
      durationSeconds: timerSeconds.value
    })

    if (response.data && response.data.data) {
      const serverResult = response.data.data
      const srvMap = {}
      for (const s of (serverResult.questions || [])) {
        srvMap[s.questionId] = s
      }
      let correct = 0
      let wrong = 0
      let unanswered = 0
      let score = 0
      let totalScore = 0
      const details = []
      const subjectMap = {}
      const tagMap = {}

      for (const q of examQuestions.value) {
        const isEssay = q.type === '综合应用'
        const userAnswer = isEssay ? '' : (examAnswers.value[q.id] || '')
        const srv = srvMap[q.id]
        const tag = q.knowledgeTag || ''

        if (isEssay) {
          // 综合应用题：只展示参考答案、不计分
          details.push({
            questionId: q.id,
            questionNumber: q.questionNumber,
            subject: q.subject,
            year: q.year || q.examYear,
            type: q.type,
            content: formatQuestionContent(q.content),
            userAnswer: '',
            answer: q.answer,
            correct: false,
            essay: true,
            score: 0,
            maxScore: 0,
            knowledgeTag: tag,
            analysis: q.analysis
          })
          continue
        }

        // 单选题：精确匹配
        const earned = srv && srv.score != null ? srv.score : (userAnswer && userAnswer === q.answer ? 2 : 0)
        const isCorrect = srv ? !!srv.isCorrect : (!!userAnswer && userAnswer === q.answer)
        const qScore = 2
        totalScore += qScore
        if (!userAnswer) {
          unanswered++
        } else if (isCorrect) {
          correct++
        } else {
          wrong++
        }
        score += earned

        if (!subjectMap[q.subject]) {
          subjectMap[q.subject] = { total: 0, correct: 0, wrong: 0, score: 0, totalScore: 0 }
        }
        subjectMap[q.subject].total++
        subjectMap[q.subject].totalScore += qScore
        if (isCorrect) {
          subjectMap[q.subject].correct++
        } else if (userAnswer) {
          subjectMap[q.subject].wrong++
        }
        subjectMap[q.subject].score += earned

        if (!tagMap[tag]) tagMap[tag] = { total: 0, correct: 0, wrong: 0, score: 0, totalScore: 0, subject: '' }
        tagMap[tag].total++
        tagMap[tag].totalScore += qScore
        if (isCorrect) tagMap[tag].correct++
        else if (userAnswer) tagMap[tag].wrong++
        tagMap[tag].score += earned
        if (!tagMap[tag].subject) tagMap[tag].subject = q.subject

        details.push({
          questionId: q.id,
          questionNumber: q.questionNumber,
          subject: q.subject,
          year: q.year || q.examYear,
          type: q.type,
          content: formatQuestionContent(q.content),
          userAnswer,
          answer: q.answer,
          correct: isCorrect,
          score: earned,
          maxScore: qScore,
          knowledgeTag: tag,
          analysis: q.analysis
        })
      }
      const accuracy = totalScore > 0 ? Math.round((score / totalScore) * 100) : 0
      const subjectBreakdown = Object.keys(subjectMap).map(s => ({
        subject: s,
        total: subjectMap[s].total,
        correct: subjectMap[s].correct,
        wrong: subjectMap[s].wrong,
        score: subjectMap[s].score,
        totalScore: subjectMap[s].totalScore,
        accuracy: subjectMap[s].totalScore > 0 ? Math.round((subjectMap[s].score / subjectMap[s].totalScore) * 100) : 0
      }))
      const knowledgeBreakdown = Object.keys(tagMap).map(t => ({
        tag: t || '未标注知识点',
        subject: tagMap[t].subject,
        total: tagMap[t].total,
        correct: tagMap[t].correct,
        wrong: tagMap[t].wrong,
        score: tagMap[t].score,
        totalScore: tagMap[t].totalScore,
        accuracy: tagMap[t].totalScore > 0 ? Math.round((tagMap[t].score / tagMap[t].totalScore) * 100) : 0
      })).sort((a, b) => a.accuracy - b.accuracy)
      examResult.value = {
        score,
        totalScore,
        correct,
        wrong,
        unanswered,
        accuracy,
        duration: timerSeconds.value,
        details,
        subjectBreakdown,
        knowledgeBreakdown
      }
      result.value = {
        total: examQuestions.value.length,
        correct,
        wrong,
        unanswered,
        accuracy,
        score,
        totalScore,
        duration: timerSeconds.value,
        details,
        subjectBreakdown,
        knowledgeBreakdown
      }
      phase.value = 'result'
      if (document.fullscreenElement) {
        try { document.exitFullscreen() } catch (e) { }
      }
    }
  } catch (e) {
    console.error('提交失败:', e)
    alert('提交失败，请重试')
  }
  submitting.value = false
}

function getOptionClass(key) {
  if (!practiceRevealed.value) {
    return practiceUserAnswer.value === key ? 'selected' : ''
  }
  const isCorrect = key === curPracticeQ.value?.answer
  const isSelected = practiceUserAnswer.value === key
  if (isCorrect) return 'correct'
  if (isSelected) return 'wrong'
  return ''
}

function selectPracticeOption(key) {
  if (practiceRevealed.value) return
  practiceUserAnswer.value = key
  practiceRevealed.value = true
  practiceIsCorrect.value = key === curPracticeQ.value?.answer
  curPracticeQ.value._answered = true
  curPracticeQ.value._correct = practiceIsCorrect.value
  curPracticeQ.value._userAnswer = key
  curPracticeQ.value._revealed = true
  checkPracticeComplete()
  savePracticeResume()

  api.saveAnswer({
    mode: 'practice',
    subject: form.value.subject,
    answers: [{ questionId: curPracticeQ.value.id, userAnswer: key }]
  }).catch(e => console.error('保存答案失败:', e))
}

function revealPracticeAnswer() {
  if (practiceRevealed.value) return
  practiceRevealed.value = true
  practiceIsCorrect.value = false
  curPracticeQ.value._revealed = true
  curPracticeQ.value._answered = false
  checkPracticeComplete()
  savePracticeResume()
}

function prevPracticeQuestion() {
  if (practiceIndex.value > 0) {
    practiceIndex.value--
    loadCurrentPracticeQ()
    savePracticeResume()
    scrollToQuestionTop()
  }
}

function nextPracticeQuestion() {
  if (practiceIndex.value < practiceQuestions.value.length - 1) {
    practiceIndex.value++
    loadCurrentPracticeQ()
    savePracticeResume()
    scrollToQuestionTop()
  }
}

/** 拖动进度条直接跳题 */
function jumpToPracticeQuestion(e) {
  const target = parseInt(e?.target?.value, 10)
  if (isNaN(target)) return
  const idx = Math.min(Math.max(target - 1, 0), practiceQuestions.value.length - 1)
  if (idx === practiceIndex.value) return
  practiceIndex.value = idx
  loadCurrentPracticeQ()
  savePracticeResume()
  scrollToQuestionTop()
}

/** 切题后回到题目顶部，避免停留在上一题中部 */
function scrollToQuestionTop() {
  try {
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e) {
    window.scrollTo(0, 0)
  }
}

function loadCurrentPracticeQ() {
  const q = curPracticeQ.value
  if (q) {
    practiceRevealed.value = q._revealed || false
    practiceUserAnswer.value = q._userAnswer || ''
    practiceTextAnswer.value = q._textAnswer || ''
    practiceIsCorrect.value = q._correct || false
  }
}

function switchPracticeSubject(subject) {
  if (subject === form.value.subject) return
  form.value.subject = subject
  form.value.knowledgeTag = ''
  knowledgeTags.value = []
  loadPracticeQuestions()
}

async function switchPracticeType(type) {
  if (type === form.value.type) return
  form.value.type = type
  await loadPracticeQuestions()
}

function switchPracticeView(view) {
  if (view === practiceMode.value) return
  practiceMode.value = view
  practiceIndex.value = 0
  savePracticeResume()
}

function getListItemOptionClass(q, key) {
  if (!q._revealed) {
    return q._userAnswer === key ? 'selected' : ''
  }
  const isCorrect = key === q.answer
  const isSelected = q._userAnswer === key
  if (isCorrect) return 'correct'
  if (isSelected) return 'wrong'
  return ''
}

function onListItemOptionClick(q, key) {
  if (q._revealed) return
  q._userAnswer = key
  q._revealed = true
  q._correct = key === q.answer
  q._answered = true
  checkPracticeComplete()
  savePracticeResume()

  api.saveAnswer({
    mode: 'practice',
    subject: form.value.subject,
    answers: [{ questionId: q.id, userAnswer: key }]
  }).catch(e => console.error('保存答案失败:', e))
}

function revealListItemAnswer(q) {
  if (q._revealed) return
  q._revealed = true
  q._answered = false
  q._correct = false
  checkPracticeComplete()
  savePracticeResume()
}

function checkPracticeComplete() {
  const currentTag = knowledgeTagFilter.value || form.value.knowledgeTag
  if (!currentTag) return

  // 综合应用题仅展示答案，不计入"完成"统计
  const statQuestions = practiceQuestions.value.filter(q => q.type === '单选')
  const allAnswered = statQuestions.length > 0 &&
    statQuestions.every(q => q._answered)
  
  if (allAnswered) {
    const correctCount = statQuestions.filter(q => q._correct).length
    showCompleteModal.value = true
    completeModalData.value = {
      total: statQuestions.length,
      correct: correctCount,
      wrong: statQuestions.length - correctCount,
      tag: currentTag
    }
  }
}

function closeCompleteModal() {
  showCompleteModal.value = false
}

function goToOverview() {
  showCompleteModal.value = false
  clearPracticeResume()
  router.push('/overview')
}

function getEncouragementMessage(correct, total) {
  const rate = correct / total
  if (rate === 1) return '完美！你对这个知识点掌握得非常扎实！🌟'
  if (rate >= 0.9) return '非常优秀！只错了一点点，继续保持！💪'
  if (rate >= 0.8) return '做得很好！继续努力，争取更高正确率！👍'
  if (rate >= 0.6) return '还不错！多复习错题，争取更好的成绩！📚'
  if (rate >= 0.4) return '需要多花时间练习，加油！🔥'
  return '不要灰心，坚持练习一定能掌握！🎯'
}

function getExamEncouragement(score, totalScore, accuracy) {
  if (!totalScore) return ''
  const rate = accuracy / 100
  if (rate >= 0.9) return '🏆 实力超群！你已经具备了冲击高分的实力，保持这个状态！'
  if (rate >= 0.75) return '🎯 表现优秀！基础扎实，继续保持做题节奏，冲刺更高分！'
  if (rate >= 0.6) return '💪 不错！距离及格线以上有不错的表现，还有提升空间！'
  if (rate >= 0.45) return '📖 及格边缘！知识点还有一些漏洞，建议针对性复习错题和薄弱科目。'
  if (rate >= 0.3) return '🌱 别灰心！现在发现问题是好事，把每道错题都搞懂，进步会非常快！'
  return '🔥 加油！现在的分数不代表最终实力，从基础开始系统性复习，你一定可以的！'
}

function getScore(q) {
  if (!q) return 0
  if (q.type === '综合应用') return 9
  return 2
}

function resetToMode() {
  phase.value = 'mode_select'
  selectedMode.value = ''
  clearPracticeResume()
  practiceQuestions.value = []
  practiceIndex.value = 0
  examQuestions.value = []
  examAnswers.value = {}
  examTextAnswers.value = {}
  examIndex.value = 0
  timerSeconds.value = 0
  showSubmitConfirm.value = false
  showAnswerCard.value = false
}

function parseOptions(options, questionNumber, year) {
  if (!options) {
    return ['A', 'B', 'C', 'D'].map(key => ({
      key: key,
      text: '',
      image: getOptionImage(year, questionNumber, key)
    }))
  }
  try {
    const parsed = JSON.parse(options)
    if (Array.isArray(parsed)) {
      const result = parsed.map(opt => ({
        key: opt.key || opt.optionKey || '',
        text: opt.text || opt.optionText || '',
        image: getOptionImage(year, questionNumber, opt.key || opt.optionKey || '')
      })).filter(opt => opt.key)
      return result.length > 0 ? result : ['A', 'B', 'C', 'D'].map(key => ({ key, text: '', image: getOptionImage(year, questionNumber, key) }))
    }
  } catch (e) {}
  const result = options.split('\n').map(opt => {
    const match = opt.match(/^([A-D])\.\s*(.+)/)
    return match ? { 
      key: match[1], 
      text: match[2],
      image: getOptionImage(year, questionNumber, match[1])
    } : null
  }).filter(Boolean)
  return result.length > 0 ? result : ['A', 'B', 'C', 'D'].map(key => ({ key, text: '', image: getOptionImage(year, questionNumber, key) }))
}

function getQuestionImages(year, questionNumber) {
  if (!year || !questionNumber) return []
  const num = parseInt(questionNumber)
  if (isNaN(num)) return []
  const knownQuestionImages = {
    '200903': ['200903.png'],
    '200904': ['200904.png']
  }
  const key = `${year}${String(num).padStart(2, '0')}`
  if (knownQuestionImages[key]) {
    return knownQuestionImages[key].map(f => `/images/questions/${f}`)
  }
  return []
}

function getOptionImage(year, questionNumber, optionKey) {
  return null
}

function imageExists(path) {
  if (!path) return false
  const knownImages = [
    '200903.png', '200904.png'
  ]
  return knownImages.some(img => path.endsWith(img))
}

function parsePageImg(content) {
  if (!content) return null
  // 支持两种格式：[page_img=xxx] 和 <!-- page_img:xxx -->
  let match = content.match(/\[page_img=([^\]]+)\]/)
  if (match) {
    return `/images/pages/${match[1]}`
  }
  match = content.match(/<!--\s*page_img:([^\s]+)\s*-->/);
  if (match) {
    return match[1]
  }
  return null
}

onMounted(() => {
  user.value = JSON.parse(localStorage.getItem('user') || 'null')
  readPracticeResume()

  const knowledgeTag = route.query.knowledgeTag
  const subject = route.query.subject

  if (knowledgeTag) {
    form.value.knowledgeTag = knowledgeTag
    form.value.subject = subject || '数据结构'
    selectedMode.value = 'smart'
    practiceMode.value = 'list'
    phase.value = 'practice'
    // 若存在相同科目+知识点的中断记录，自动恢复进度
    if (resumeInfo.value && resumeInfo.value.mode === 'smart' &&
        resumeInfo.value.subject === form.value.subject &&
        resumeInfo.value.knowledgeTag === knowledgeTag) {
      resumePractice()
    } else {
      loadSmartQuestions()
    }
    fetchYears()
  } else if (route.query.year) {
    form.value.year = route.query.year
    selectedMode.value = 'exam'
    phase.value = 'setup'
    fetchYears()
  }

  const persist = () => savePracticeResume()
  window.addEventListener('beforeunload', persist)
  window.addEventListener('pagehide', persist)
})

// 已在 /practice 页面内（如结果页）点击「生成该知识点练习」时，路由仅 query 变化，组件不会重新挂载，
// 需监听 query 完成同样的初始化逻辑
watch(() => route.query, (nq) => {
  const knowledgeTag = nq.knowledgeTag
  if (!knowledgeTag) return
  form.value.knowledgeTag = knowledgeTag
  form.value.subject = nq.subject || '数据结构'
  selectedMode.value = 'smart'
  practiceMode.value = 'list'
  phase.value = 'practice'
  result.value = { total: 0, correct: 0, wrong: 0, accuracy: 0, score: 0, duration: 0, details: [] }
  if (resumeInfo.value && resumeInfo.value.mode === 'smart' &&
      resumeInfo.value.subject === form.value.subject &&
      resumeInfo.value.knowledgeTag === knowledgeTag) {
    resumePractice()
  } else {
    loadSmartQuestions()
  }
})

onBeforeUnmount(() => {
  stopTimer()
  window.removeEventListener('beforeunload', savePracticeResume)
  window.removeEventListener('pagehide', savePracticeResume)
})
</script>

<style scoped>
.ai-gen-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; margin-bottom: 12px;
  background: rgba(124, 58, 237, 0.06); border: 1px solid rgba(124, 58, 237, 0.2);
  border-radius: 8px;
}
.practice-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.top-nav {
  background: #fff;
  padding: 0 24px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  font-size: 17px;
  font-weight: 700;
  color: #1a73e8;
  white-space: nowrap;
}

.nav-center {
  display: flex;
  gap: 24px;
}

.nav-center a {
  text-decoration: none;
  color: #333;
  font-size: 14px;
  padding: 6px 8px;
  border-radius: 6px;
}

.nav-center a:hover {
  background: rgba(26, 115, 232, 0.1);
  color: #1a73e8;
}

.nav-center a.active {
  background: #1a73e8;
  color: #fff;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-nickname {
  font-size: 14px;
  color: #666;
}

.nav-logout-btn {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
  background: #fff;
}

.nav-logout-btn:hover {
  border-color: #e53935;
  color: #e53935;
}

.nav-theme-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
}

.nav-theme-btn:hover {
  background: rgba(26, 115, 232, 0.1);
}

.practice-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 12px 18px;
  border-radius: 12px;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.practice-mode-label {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.practice-header-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.ph-btn {
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.4);
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s ease;
  white-space: nowrap;
}

.ph-btn:hover {
  background: rgba(255, 255, 255, 0.32);
}

.ph-btn-danger {
  border-color: rgba(255, 120, 120, 0.6);
  color: #ffe3e3;
}

.ph-btn-danger:hover {
  background: rgba(220, 38, 38, 0.35);
}

.mode-badge {
  background: rgba(255, 255, 255, 0.25);
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
}

.mode-badge.smart-mode {
  background: rgba(76, 175, 80, 0.9);
}

.knowledge-tag-label {
  font-size: 14px;
  opacity: 0.9;
}

.practice-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* ===== 考试模式沉浸式 ===== */
.practice-page.exam-mode {
  background: #e8ecef;
}

.practice-page.exam-mode .practice-main {
  padding: 0;
  background: #e8ecef;
}

.practice-page.exam-mode .exam-header {
  position: sticky;
  top: 0;
  z-index: 100;
  margin: 0;
  padding: 14px 24px;
  border-radius: 0;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid #d0d7de;
}

.practice-page.exam-mode .question-item {
  max-width: 760px;
  margin: 20px auto;
  padding: 24px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.practice-page.exam-mode .exam-nav {
  max-width: 800px;
  margin: 0 auto 40px auto;
  padding: 16px 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 22px;
  color: #1a73e8;
}

.mode-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
  max-width: 1000px;
  margin: 0 auto;
}

.mode-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 24px;
  text-align: center;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.mode-card:hover {
  border-color: #1a73e8;
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(26, 115, 232, 0.2);
}

.mode-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.mode-name {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
}

.mode-desc {
  font-size: 15px;
  color: #666;
  line-height: 1.6;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  max-width: 500px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #1a73e8;
}

.tab-bar {
  display: flex;
  gap: 8px;
}

.tab-btn {
  padding: 6px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
  background: #fff;
  transition: all 0.3s ease;
}

.tab-btn:hover {
  border-color: #1a73e8;
  color: #1a73e8;
  transform: translateY(-1px);
}

.tab-btn.active {
  background: #1a73e8;
  color: #fff;
  border-color: #1a73e8;
  box-shadow: 0 2px 8px rgba(26, 115, 232, 0.3);
  transform: translateY(-1px);
}

.exam-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}

.exam-timer {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.exam-timer.warning {
  color: #f57c00;
}

.question-item {
  background: #fff;
  border-radius: 14px;
  padding: 22px 24px;
  margin-bottom: 18px;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.question-header {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
}

.question-badge {
  background: #f1f3f4;
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  color: #666;
}

.question-number {
  font-size: 14px;
  color: #999;
}

.question-content {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  margin-bottom: 16px;
}

.options-list {
  list-style: none;
  padding: 0;
  margin: 24px auto 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  max-width: 85%;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  border: 2px solid #e5e7eb;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: #f9fafb;
  font-size: 16px;
  line-height: 1.6;
}

.option-item:hover {
  border-color: #1a73e8;
  background: #f8fafc;
}

.option-item.selected {
  border-color: #4caf50;
  background: #f1f8e9;
}

.option-item.correct {
  border-color: #4caf50;
  background: #e8f5e9;
}

.option-item.wrong {
  border-color: #f44336;
  background: #ffebee;
}

.option-key {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #10b981;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.option-item.selected .option-key {
  background: #4caf50;
}

.option-item.correct .option-key {
  background: #4caf50;
}

.option-item.wrong .option-key {
  background: #f44336;
}

.options-images-container {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  justify-content: space-around;
}

.option-image-item {
  flex: 1;
  min-width: 150px;
  max-width: 200px;
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
}

.option-image-item img {
  width: 100%;
  display: block;
}

.option-label {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
}

.analysis-box {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 8px 12px;
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
}

.analysis-box .an-item {
  display: inline;
  margin-right: 16px;
  white-space: pre-wrap;
}

.analysis-box.an-locked {
  position: relative;
  overflow: hidden;
}

.analysis-box.an-locked .an-item,
.analysis-box.an-locked > div {
  filter: blur(7px);
  opacity: 0.45;
  user-select: none;
  pointer-events: none;
}

.an-lock-btn {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #059669;
  color: #fff;
  border: none;
  border-radius: 18px;
  padding: 6px 16px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  z-index: 2;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  white-space: nowrap;
}

.an-lock-btn:hover {
  background: #037a55;
}

.resume-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: linear-gradient(135deg, rgba(5, 150, 105, 0.08), rgba(245, 124, 0, 0.08));
  border: 1px solid rgba(5, 150, 105, 0.35);
  border-radius: 10px;
  padding: 12px 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.resume-title {
  font-weight: 600;
  font-size: 15px;
  color: var(--text);
}
.resume-desc {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 2px;
}
body.dark-mode .resume-banner {
  background: rgba(5, 150, 105, 0.12);
  border-color: #0d9488;
}

.ai-score-box {
  background: linear-gradient(135deg, rgba(124,58,237,0.08), rgba(192,132,252,0.08));
  border: 1px solid rgba(124, 58, 237, 0.25);
  border-radius: 8px;
  padding: 12px 16px;
  margin-top: 10px;
}

.ai-score-title {
  font-size: 14px;
  color: #333;
}

.ai-feedback {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.7;
  color: #555;
  white-space: pre-wrap;
}

body.dark-mode .ai-score-title { color: #e2e8f0; }
body.dark-mode .ai-feedback { color: #cbd5e1; }

.exam-nav {
  margin-top: 20px;
}

.nav-buttons {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background: #1a73e8;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #1557b0;
}

.btn-outline {
  background: #fff;
  color: #1a73e8;
  border: 2px solid #1a73e8;
}

.btn-outline:hover:not(:disabled) {
  background: #e3f2fd;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 13px;
}

.btn-lg {
  padding: 14px 28px;
  font-size: 16px;
}

.btn-block {
  width: 100%;
}

.answer-card-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.answer-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 90%;
  max-width: 500px;
}

.answer-card h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
}

.answer-grid {
  display: grid;
  grid-template-columns: repeat(10, 1fr);
  gap: 8px;
}

.answer-card-btn {
  width: 40px;
  height: 40px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  background: #fff;
}

.answer-card-btn:hover {
  border-color: #1a73e8;
}

.answer-card-btn.answered {
  background: #1a73e8;
  color: #fff;
  border-color: #1a73e8;
}

.confirm-modal {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 90%;
  max-width: 420px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.confirm-modal h3 {
  font-size: 18px;
  color: var(--text);
}

.practice-switcher {
  display: flex;
  gap: 16px;
  margin-bottom: 0;
  flex-wrap: wrap;
}

.practice-subject-tabs {
  display: flex;
  gap: 4px;
}

.practice-subject-tab {
  padding: 6px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
  background: #fff;
}

.practice-subject-tab:hover {
  border-color: #1a73e8;
  color: #1a73e8;
}

.practice-subject-tab.active {
  background: #1a73e8;
  color: #fff;
  border-color: #1a73e8;
}

.practice-type-tabs {
  display: flex;
  gap: 4px;
}

.practice-type-tab {
  padding: 6px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
  background: #fff;
}

.practice-type-tab:hover {
  border-color: #f57c00;
  color: #f57c00;
}

.practice-type-tab.active {
  background: #f57c00;
  color: #fff;
  border-color: #f57c00;
}

.practice-view-tabs {
  display: flex;
  gap: 4px;
}

.practice-list-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.list-mode-item {
  margin-bottom: 0;
}

.practice-loading {
  text-align: center;
  padding: 40px;
  font-size: 16px;
  color: #666;
}

.practice-empty {
  text-align: center;
  padding: 40px;
  font-size: 16px;
  color: #666;
  background: #fff;
  border-radius: 12px;
}

.complete-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 200;
}

.complete-modal {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  width: 90%;
  max-width: 400px;
  text-align: center;
}

.complete-modal-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.complete-modal-title {
  font-size: 24px;
  margin: 0 0 8px 0;
  color: #333;
}

.complete-modal-desc {
  font-size: 14px;
  color: #666;
  margin: 0 0 20px 0;
}

.complete-modal-stats {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
}

.stat-num {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.stat-num.correct {
  color: #4caf50;
}

.stat-num.wrong {
  color: #f44336;
}

.stat-label {
  font-size: 12px;
  color: #999;
}

.complete-modal-message {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
  font-size: 14px;
  line-height: 1.6;
  color: #3c4043;
}

.complete-modal-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.result-page {
  max-width: 700px;
  margin: 0 auto;
}

.result-header {
  text-align: center;
  padding: 32px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 16px;
  margin-bottom: 20px;
}

.result-header.excellent {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.result-header.good {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.result-header.pass {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}

.result-header.low {
  background: linear-gradient(135deg, #434343 0%, #000000 100%);
}

.result-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 12px auto;
  background: rgba(255,255,255,0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.result-header h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
}

.result-header p {
  margin: 0;
  opacity: 0.9;
}

.result-message {
  background: rgba(255,255,255,0.15);
  padding: 12px 16px;
  border-radius: 8px;
  margin-top: 16px;
  font-size: 14px;
  line-height: 1.6;
}

.result-message.low-score {
  background: rgba(255,255,255,0.25);
  font-weight: 500;
}

.result-score-display {
  background: #fff;
  border-radius: 16px;
  padding: 28px;
  text-align: center;
  margin-bottom: 20px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}

.score-big {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  margin-bottom: 8px;
}

.score-num {
  font-size: 72px;
  font-weight: 800;
  color: var(--primary, #f57c00);
  line-height: 1;
}

.score-sep {
  font-size: 32px;
  color: #ccc;
  font-weight: 300;
}

.score-total {
  font-size: 36px;
  font-weight: 300;
  color: #999;
}

.score-label {
  font-size: 14px;
  color: #999;
  letter-spacing: 2px;
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.result-stat {
  text-align: center;
  background: #fff;
  padding: 16px 8px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.result-stat-num {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.result-stat-num.correct {
  color: #4caf50;
}

.result-stat-num.wrong {
  color: #f44336;
}

.result-stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  display: block;
}

.result-analysis {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.result-analysis h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
}

.subject-breakdown {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.subject-breakdown:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.result-detail {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 8px;
}

.result-detail.correct {
  background: #e8f5e9;
}

.result-detail.wrong {
  background: #ffebee;
}

.result-detail.wrong-detail {
  background: #fff;
  border: 1px solid #ffcdd2;
  align-items: flex-start;
  padding: 16px;
}

.detail-num {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.detail-info {
  flex: 1;
  font-size: 13px;
  color: #333;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.detail-answer {
  font-size: 13px;
  color: #666;
  text-align: right;
  min-width: 120px;
}

@media (max-width: 600px) {
  .result-stats {
    grid-template-columns: repeat(2, 1fr);
  }
  .score-num {
    font-size: 56px;
  }
  .score-total {
    font-size: 28px;
  }
}

@media (max-width: 900px) {
  .nav-center {
    display: none;
  }
  .mode-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== 深色模式 ===== */
body.dark-mode .practice-page { background: #0f172a; }
body.dark-mode .top-nav { background: #1e293b; box-shadow: 0 1px 3px rgba(0,0,0,0.3); }
body.dark-mode .logo { color: #60a5fa; }
body.dark-mode .nav-center a { color: #cbd5e1; }
body.dark-mode .nav-center a:hover { background: rgba(96, 165, 250, 0.15); color: #60a5fa; }
body.dark-mode .nav-center a.active { background: #3b82f6; color: #fff; }
body.dark-mode .nav-nickname { color: #94a3b8; }
body.dark-mode .nav-logout-btn { background: #1e293b; border-color: #334155; color: #94a3b8; }
body.dark-mode .nav-logout-btn:hover { border-color: #ef4444; color: #ef4444; }
body.dark-mode .nav-theme-btn:hover { background: rgba(96, 165, 250, 0.15); }
body.dark-mode .page-header h2 { color: #60a5fa; }
body.dark-mode .mode-card { background: #1e293b; border-color: #334155; }
body.dark-mode .mode-card:hover { border-color: #3b82f6; box-shadow: 0 8px 24px rgba(59, 130, 246, 0.2); }
body.dark-mode .mode-name { color: #f1f5f9; }
body.dark-mode .mode-desc { color: #94a3b8; }
body.dark-mode .mode-card-inner { color: #cbd5e1; }
body.dark-mode .exam-setup { background: #1e293b; border-color: #334155; }
body.dark-mode .setup-title { color: #f1f5f9; }
body.dark-mode .setup-row label { color: #cbd5e1; }
body.dark-mode .year-select, body.dark-mode .form-select { background: #0f172a; color: #f1f5f9; border-color: #334155; }
body.dark-mode .setup-info { background: rgba(96, 165, 250, 0.1); color: #93c5fd; }
body.dark-mode .smart-setup { background: #1e293b; border-color: #334155; }
body.dark-mode .smart-title { color: #f1f5f9; }
body.dark-mode .form-field label { color: #cbd5e1; }
body.dark-mode .form-input, body.dark-mode .form-select, body.dark-mode input[type="text"], body.dark-mode select { background: #0f172a; color: #f1f5f9; border-color: #334155; }
body.dark-mode .exam-container { background: #1e293b; border-color: #334155; }
body.dark-mode .exam-header { border-bottom-color: #334155; }
body.dark-mode .practice-page.exam-mode { background: #020617; }
body.dark-mode .practice-page.exam-mode .practice-main { background: #020617; }
body.dark-mode .practice-page.exam-mode .exam-header { background: #0f172a; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.4); border-bottom: 1px solid #1e293b; }
body.dark-mode .practice-page.exam-mode .question-item { background: #0f172a; border-color: #1e293b; box-shadow: 0 2px 12px rgba(0, 0, 0, 0.4); }
body.dark-mode .exam-title { color: #f1f5f9; }
body.dark-mode .exam-info { color: #94a3b8; }
body.dark-mode .question-content-text { color: #e2e8f0; }
body.dark-mode .question-content { color: #e2e8f0; }
body.dark-mode .practice-page.exam-mode .exam-header .btn-outline { background: #1e293b; border-color: #334155; color: #e2e8f0; }
body.dark-mode .practice-page.exam-mode .exam-header .btn-outline:hover { background: #334155; border-color: #3b82f6; color: #60a5fa; }
body.dark-mode .exam-timer.warning { color: #f87171 !important; }
body.dark-mode .option-item { background: #0f172a; border-color: #334155; color: #e2e8f0; }
body.dark-mode .option-item:hover { background: #1e3a5f; border-color: #3b82f6; }
body.dark-mode .option-item.selected { background: rgba(59, 130, 246, 0.15); border-color: #3b82f6; }
body.dark-mode .option-key { background: #334155; color: #e2e8f0; }
body.dark-mode .text-answer-input { background: #0f172a; color: #f1f5f9; border-color: #334155; }
body.dark-mode .exam-actions { border-top-color: #334155; }
body.dark-mode .exam-timer { color: #e2e8f0; }
body.dark-mode .smart-setup-box, body.dark-mode .exam-status-box { background: #1e293b; border-color: #334155; }
body.dark-mode .smart-setup-hint { color: #94a3b8; }
body.dark-mode .question-list { background: #1e293b; border-color: #334155; }
body.dark-mode .question-item { background: #0f172a; border-color: #334155; }
body.dark-mode .question-number { color: #f1f5f9; }
body.dark-mode .question-badge { background: rgba(16, 185, 129, 0.2) !important; color: #34d399 !important; }
body.dark-mode .question-analysis { background: rgba(245, 158, 11, 0.1); color: #fcd34d; }
body.dark-mode .analysis-answer { color: #34d399; }
body.dark-mode .exam-paper { background: #1e293b; border-color: #334155; }
body.dark-mode .exam-paper-header { border-bottom-color: #334155; }
body.dark-mode .exam-paper-title { color: #f1f5f9; }
body.dark-mode .exam-paper-subtitle { color: #94a3b8; }
body.dark-mode .subject-divider { background: #334155; }
body.dark-mode .subject-title { color: #60a5fa; }
body.dark-mode .q-num-btn { background: #0f172a; border-color: #334155; color: #e2e8f0; }
body.dark-mode .q-num-btn:hover { background: #1e3a5f; border-color: #3b82f6; }
body.dark-mode .q-num-btn.current { background: #3b82f6; color: #fff; }
body.dark-mode .q-num-btn.answered { background: rgba(16, 185, 129, 0.3); border-color: #10b981; color: #34d399; }
body.dark-mode .q-num-btn.correct { background: rgba(16, 185, 129, 0.3); border-color: #10b981; color: #34d399; }
body.dark-mode .q-num-btn.wrong { background: rgba(239, 68, 68, 0.3); border-color: #ef4444; color: #f87171; }
body.dark-mode .exam-sidebar { background: #1e293b; border-color: #334155; }
body.dark-mode .sidebar-section-title { color: #f1f5f9; }
body.dark-mode .legend-item { color: #94a3b8; }
body.dark-mode .legend-box { border-color: #334155; }
body.dark-mode .answer-btn-row { border-top-color: #334155; }
body.dark-mode .modal-overlay { background: rgba(0, 0, 0, 0.7); }
body.dark-mode .complete-modal { background: #1e293b; border-color: #334155; }
body.dark-mode .complete-modal-title { color: #f1f5f9; }
body.dark-mode .complete-modal-desc { color: #94a3b8; }
body.dark-mode .stat-num { color: #f1f5f9; }
body.dark-mode .stat-label { color: #94a3b8; }
body.dark-mode .complete-modal-message { background: #0f172a; color: #cbd5e1; }
body.dark-mode .result-header.excellent, body.dark-mode .result-header.good, body.dark-mode .result-header.pass { opacity: 0.9; }
body.dark-mode .result-stats { background: #1e293b; border-color: #334155; }
body.dark-mode .result-stat-item { color: #e2e8f0; }
body.dark-mode .score-section { background: #1e293b; border-color: #334155; }
body.dark-mode .subject-score-title { color: #f1f5f9; }
body.dark-mode .score-detail-row { border-bottom-color: #334155; }
body.dark-mode .subject-name { color: #cbd5e1; }
body.dark-mode .wrong-list-section, body.dark-mode .full-list-section { background: #1e293b; border-color: #334155; }
body.dark-mode .question-detail { background: #0f172a; border-color: #334155; }
body.dark-mode .question-detail-header { border-bottom-color: #334155; }
body.dark-mode .question-type { color: #94a3b8; }
body.dark-mode .question-detail-content { color: #e2e8f0; }
body.dark-mode .question-detail-options .option-item { background: #0f172a; border-color: #334155; color: #e2e8f0; }
body.dark-mode .question-detail-answer { background: rgba(16, 185, 129, 0.15); border-color: #10b981; color: #34d399; }
body.dark-mode .answer-explanation { color: #cbd5e1; }
body.dark-mode .answer-result { color: #e2e8f0; }
body.dark-mode .answer-result.correct { color: #34d399; }
body.dark-mode .answer-result.wrong { color: #f87171; }
body.dark-mode .btn-outline { background: #0f172a; border-color: #334155; color: #e2e8f0; }
body.dark-mode .btn-outline:hover { background: #1e3a5f; border-color: #3b82f6; color: #60a5fa; }
body.dark-mode .btn-outline:disabled, body.dark-mode .btn-outline.disabled { background: #334155; color: #64748b; border-color: #475569; }
body.dark-mode .exam-paper-actions { border-top-color: #334155; }
body.dark-mode .answer-score { color: #94a3b8; }
body.dark-mode .question-count { color: #94a3b8; }
body.dark-mode .answer-card { background: #1e293b; color: #f1f5f9; }
body.dark-mode .answer-card h3 { color: #f1f5f9; }
body.dark-mode .answer-card-btn { background: #0f172a; border-color: #334155; color: #e2e8f0; }
body.dark-mode .answer-card-btn:hover { border-color: #3b82f6; }
body.dark-mode .answer-card-btn.answered { background: #3b82f6; color: #fff; border-color: #3b82f6; }
body.dark-mode .confirm-modal { background: #1e293b; color: #f1f5f9; }
body.dark-mode .confirm-modal h3 { color: #f1f5f9; }

/* ===== 单题模式底部翻页条 ===== */
.question-pager {
  position: sticky;
  bottom: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding: 12px 18px;
  background: var(--bg-card, #fff);
  border: 1px solid var(--border, #e5e7eb);
  border-radius: 12px;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.08);
}
.pager-btn {
  padding: 8px 20px;
  border-radius: 8px;
  border: 1px solid var(--border, #e5e7eb);
  background: transparent;
  color: var(--text, #1f2937);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}
.pager-btn:hover:not(:disabled) { border-color: #059669; color: #059669; }
.pager-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.pager-btn-primary {
  background: #059669;
  border-color: #059669;
  color: #fff;
}
.pager-btn-primary:hover:not(:disabled) { background: #047857; border-color: #047857; color: #fff; }
.pager-progress {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.pager-index {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary, #6b7280);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}
.pager-slider {
  flex: 1;
  min-width: 60px;
  accent-color: #059669;
  cursor: pointer;
}
body.dark-mode .question-pager { background: #1e293b; border-color: #334155; }
body.dark-mode .pager-btn { color: #e2e8f0; border-color: #334155; }
body.dark-mode .pager-btn:hover:not(:disabled) { border-color: #10b981; color: #10b981; }
body.dark-mode .pager-index { color: #94a3b8; }
@media (max-width: 640px) {
  .question-pager { gap: 8px; padding: 10px 12px; }
  .pager-btn { padding: 8px 12px; font-size: 13px; }
  .pager-slider { display: none; }
}
</style>
