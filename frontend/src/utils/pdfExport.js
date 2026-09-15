export function escapeHtml(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export function questionBlock(it) {
  const year = it.year ? `<span class="badge">${escapeHtml(String(it.year))}</span>` : ''
  const kw = it.knowledgeTag ? `<span class="badge">${escapeHtml(it.knowledgeTag)}</span>` : ''
  const optList = (it.options && it.options.length)
    ? '<div class="opts">' + it.options.map(o => `<div class="opt"><span class="opt-key">${escapeHtml(o.key)}.</span> ${escapeHtml(o.text || '')}</div>`).join('') + '</div>'
    : ''
  const essay = !!it.essay
  let contentBody
  if (typeof it.content === 'string' && it.content.indexOf('<') >= 0) {
    contentBody = it.content
  } else {
    contentBody = '<div class="plain">' + escapeHtml(it.content) + '</div>'
  }
  const userAns = (it.userAnswer && !essay)
    ? `<div class="ans"><b>你的答案：</b>${escapeHtml(it.userAnswer)}　<b>正确答案：</b>${escapeHtml(it.answer)}</div>`
    : `<div class="ans"><b>正确答案：</b>${escapeHtml(it.answer)}</div>`
  const tip = it.correct === false && !essay ? '<span class="mark wrong">✗</span>' : (it.correct ? '<span class="mark right">✓</span>' : '')
  const analysis = it.analysis ? `<div class="analysis">【解析】${escapeHtml(it.analysis)}</div>` : ''
  return `<div class="q">
  <div class="q-head">${year}<span class="badge">第${escapeHtml(String(it.questionNumber || ''))}题</span><span class="badge">${escapeHtml(it.type || '')}</span>${kw}${tip}</div>
  <div class="q-body">${contentBody}</div>
  ${optList}
  ${userAns}
  ${analysis}
</div>`
}

export function exportPdf({ title, subtitle, blocks }) {
  const html = [
    '<!doctype html><html><head><meta charset="utf-8"><title>' + escapeHtml(title) + '</title><style>',
    '* { box-sizing: border-box; }',
    'body { font-family: "Microsoft YaHei", "PingFang SC", Arial, sans-serif; font-size: 13px; line-height: 1.7; color: #1f2937; padding: 24px 32px; }',
    'h1 { text-align: center; font-size: 20px; margin: 8px 0 2px; }',
    '.sub { text-align: center; color: #6b7280; font-size: 12px; margin-bottom: 16px; }',
    '.q { page-break-inside: avoid; margin-bottom: 14px; padding: 12px; border: 1px solid #e5e7eb; border-radius: 8px; background: #fff; }',
    '.q-head { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; font-size: 12px; color: #555; margin-bottom: 6px; }',
    '.q-head .badge { padding: 2px 8px; border-radius: 10px; background: #f3f4f6; color: #333; }',
    '.mark { font-weight: 700; }',
    '.mark.right { color: #059669; }',
    '.mark.wrong { color: #dc2626; }',
    '.q-body { margin-bottom: 6px; }',
    '.opts { margin: 4px 0 0; }',
    '.opt { margin: 2px 0; padding-left: 4px; }',
    '.opt-key { font-weight: 600; }',
    '.ans { font-size: 12px; color: #374151; margin-top: 6px; }',
    '.ans b { color: #059669; }',
    '.analysis { font-size: 12px; color: #4b5563; margin-top: 6px; white-space: pre-wrap; border-left: 3px solid #f57c00; padding-left: 8px; }',
    'pre, code { font-family: Consolas, Menlo, monospace; font-size: 12px; white-space: pre-wrap; }',
    'pre { background: #f6f8fa; padding: 8px; border-radius: 6px; margin: 4px 0; }',
    'img { max-width: 100%; }',
    '.plain { white-space: pre-wrap; }',
    '@media print { body { padding: 8px 16px; } .q { border-color: #ddd; } }',
    '</style></head><body>',
    '<h1>' + escapeHtml(title) + '</h1>',
    subtitle ? '<div class="sub">' + escapeHtml(subtitle) + '</div>' : '',
    (blocks || []).join('\n'),
    '</body></html>'
  ].join('\n')
  const w = window.open('', '_blank')
  if (!w) return false
  w.document.open()
  w.document.write(html)
  w.document.close()
  w.focus()
  setTimeout(() => { try { w.print() } catch (e) {} }, 300)
  return true
}