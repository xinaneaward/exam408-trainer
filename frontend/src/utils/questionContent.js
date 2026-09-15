// 题目内容清洗工具：去掉占位注释、将代码块整理为 K&R 风格。
// 题目内容来自 PDF 提取，代码块常是 Pandoc 高亮产物：
//   <div class="highlight"><pre ...><code ...>
//     <span style="display:flex"><span>单行内容</span></span>…
// 这类“每行一个 flex span”的结构在 textContent 取值时会丢失换行，
// 且大括号可能为 Allman 风格。这里统一抽取文本并重排为 K&R 风格。

let inBlockComment = false

function expandTabWidth(line) {
  return line.replace(/\t/g, '    ')
}

function leadingWidth(line) {
  const m = line.match(/^[ \t]*/)
  if (!m) return 0
  let w = 0
  for (const ch of m[0]) w += ch === '\t' ? 4 : 1
  return w
}

// 将字符串 / 字符字面量 / 注释替换为等长空白，仅保留结构（花括号），
// 以便统计时忽略注释和字符串内部的花括号。
function structuralMask(line) {
  let i = 0
  const out = []
  while (i < line.length) {
    const c = line[i]
    const nxt = line[i + 1]
    if (c === '"' || c === "'") {
      const quote = c
      out.push(quote)
      i++
      while (i < line.length) {
        const cc = line[i]
        if (cc === '\\') {
          out.push(' ')
          i++
          if (i < line.length) { out.push(' '); i++ }
          continue
        }
        if (cc === quote) { out.push(quote); i++; break }
        out.push(' ')
        i++
      }
    } else if (c === '/' && nxt === '/') {
      while (i < line.length) { out.push(' '); i++ }
    } else if (c === '/' && nxt === '*') {
      out.push(' ', ' ')
      i += 2
      let closed = false
      while (i < line.length) {
        if (line[i] === '*' && line[i + 1] === '/') {
          out.push(' ', ' ')
          i += 2
          closed = true
          break
        }
        out.push(' ')
        i++
      }
      if (!closed) inBlockComment = true
    } else {
      out.push(c)
      i++
    }
  }
  return out.join('')
}

// 处理可能跨行的块注释：上一行残留 inBlockComment=true 时先整段抹白。
function maskWithState(line) {
  if (!inBlockComment) return structuralMask(line)
  const close = line.indexOf('*/')
  if (close === -1) return ' '.repeat(line.length)
  inBlockComment = false
  const rest = line.slice(close + 2)
  return ' '.repeat(close + 2) + maskWithState(rest)
}

function braceDelta(mask) {
  let open = 0
  let close = 0
  for (let k = 0; k < mask.length; k++) {
    if (mask[k] === '{') open++
    else if (mask[k] === '}') close++
  }
  return open - close
}

function indentFor(level) {
  return '    '.repeat(Math.max(0, level))
}

export function beautifyToKr(code) {
  if (!code) return ''
  inBlockComment = false
  const lines = String(code).replace(/\r/g, '').split('\n')
  const out = []
  let depth = 0

  for (const raw of lines) {
    const src = expandTabWidth(raw)
    const trimmed = src.trim()

    if (inBlockComment) {
      const mask = maskWithState(trimmed)
      if (trimmed) out.push(indentFor(depth) + trimmed)
      continue
    }

    if (trimmed === '') {
      if (out.length && out[out.length - 1] !== '') out.push('')
      continue
    }

    const mask = maskWithState(trimmed)
    const mTrim = mask.trim()
    const origIndent = leadingWidth(src)

    if (mTrim === '') {
      // 整行是字符串或注释
      if (trimmed) out.push(indentFor(depth) + trimmed)
      continue
    }

    if (mTrim[0] === '{' && mTrim.length === 1) {
      // 单独成行的开括号 → K&R：合并到上一逻辑行末尾
      let last = out.length - 1
      while (last >= 0 && out[last] === '') last--
      if (last >= 0 && out[last] !== '') {
        if (!/\{\s*$/.test(out[last])) out[last] = out[last] + ' {'
      } else {
        out.push(indentFor(depth) + '{')
      }
      depth = Math.max(0, depth + braceDelta(mTrim))
      continue
    }

    const baseDepth = mTrim[0] === '}' ? Math.max(0, depth - 1) : depth
    const indentWidth = Math.max(baseDepth * 4, origIndent)
    out.push(' '.repeat(indentWidth) + trimmed)
    depth = Math.max(0, depth + braceDelta(mTrim))
  }

  while (out.length && out[out.length - 1] === '') out.pop()
  const res = []
  for (const l of out) {
    if (l === '' && res.length && res[res.length - 1] === '') continue
    res.push(l)
  }
  return res.join('\n')
}

// 从 Pandoc 高亮代码块中抽取纯文本。
// 每行是一个 <span style="display:flex"> 容器，textContent 不会自带换行，需逐行拼接。
function extractCodeText(container) {
  const flexSpans = Array.from(container.querySelectorAll('span[style*="display"]'))
  if (flexSpans.length > 1) {
    return flexSpans.map(f => (f.textContent || '')).join('\n')
  }
  return container.textContent || ''
}

// 清洗题目/解析 HTML：去掉占位注释，把 <pre> 代码块重排为 K&R 风格。
export function formatQuestionContent(html) {
  if (!html) return ''
  let s = String(html)
    .replace(/<!--\s*page_img:.*?-->/g, '')
    .replace(/\[page_img=[^\]]+\]/g, '')
  try {
    if (typeof DOMParser === 'undefined') return s
    const doc = new DOMParser().parseFromString(s, 'text/html')
    const pres = Array.from(doc.querySelectorAll('pre'))
    for (const pre of pres) {
      const code = pre.querySelector('code')
      const text = extractCodeText(code || pre)
      const clean = beautifyToKr(text)
      const np = doc.createElement('pre')
      np.setAttribute('class', 'code-kr')
      const nc = doc.createElement('code')
      nc.textContent = clean
      np.appendChild(nc)
      pre.replaceWith(np)
    }
    const highlights = Array.from(doc.querySelectorAll('.highlight'))
    for (const el of highlights) el.replaceWith(...el.childNodes)
    return doc.body.innerHTML
  } catch (e) {
    return s
  }
}

// 解析文字格式化：代码类（含缩进行 / 函数签名行）原样保留换行；
// 散文类把压扁成正常段落流动排版，避免“隔几个字就换行”。
export function formatAnalysis(text) {
  if (!text) return text || ''
  const s = String(text)
  const looksLikeCode =
    /^\s{2,}/m.test(s) ||
    /^[A-Za-z_]\w*\s*\([^)]*\)\s*\{/m.test(s) ||
    /^(?:static|void|int|char|long|double|float|struct|class)\b/m.test(s)
  if (looksLikeCode) return s
  return s
    .replace(/[ \t]*\n[ \t]*\n[ \t]*/g, '\n')
    .replace(/[ \t]*\n[ \t]*/g, ' ')
    .replace(/[ ]{2,}/g, ' ')
    .trim()
}