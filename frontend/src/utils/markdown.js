/**
 * 轻量 Markdown 渲染器（用于 AI 回复展示）
 * 支持：代码块、行内代码、标题、加粗、斜体、有序/无序列表、段落
 * 输出已转义，可安全使用 v-html
 */

function escapeHtml(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function renderInline(s) {
  return s
    .replace(/`([^`]+)`/g, '<code class="ai-inline-code">$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
}

function renderBlock(text) {
  const lines = escapeHtml(text).split('\n')
  let html = ''
  let list = null
  const closeList = () => {
    if (list) {
      html += `</${list}>`
      list = null
    }
  }
  for (const raw of lines) {
    const line = raw.replace(/\s+$/, '')
    const ulMatch = line.match(/^\s*[-*•]\s+(.*)$/)
    const olMatch = line.match(/^\s*\d+[.、)]\s+(.*)$/)
    const h = line.match(/^(#{1,3})\s+(.*)$/)
    if (h) {
      closeList()
      html += '<h4 class="ai-h">' + renderInline(h[2]) + '</h4>'
    } else if (ulMatch) {
      if (list !== 'ul') { closeList(); html += '<ul class="ai-ul">' }
      list = 'ul'
      html += '<li>' + renderInline(ulMatch[1]) + '</li>'
    } else if (olMatch) {
      if (list !== 'ol') { closeList(); html += '<ol class="ai-ol">' }
      list = 'ol'
      html += '<li>' + renderInline(olMatch[1]) + '</li>'
    } else if (line.trim() === '') {
      closeList()
    } else {
      closeList()
      html += '<p class="ai-p">' + renderInline(line) + '</p>'
    }
  }
  closeList()
  return html
}

export function renderMarkdown(text) {
  if (!text) return ''
  const segments = String(text).split('```')
  let html = ''
  for (let i = 0; i < segments.length; i++) {
    if (i % 2 === 1) {
      // 代码块：去掉首行语言标识
      const code = segments[i].replace(/^\w*\n/, '').replace(/\n$/, '')
      html += '<pre class="ai-code"><code>' + code + '</code></pre>'
    } else {
      html += renderBlock(segments[i])
    }
  }
  return html
}
