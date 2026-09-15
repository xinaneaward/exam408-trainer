export const sleep = (ms) => new Promise(r => setTimeout(r, ms))

export function getCanvasContext(canvasRef) {
  const canvas = canvasRef.value
  if (!canvas) return null
  const ctx = canvas.getContext('2d')
  return { canvas, ctx }
}

export function clearCanvas(canvas, ctx) {
  ctx.clearRect(0, 0, canvas.width, canvas.height)
}

export function setupHiDPICanvas(canvas, width, height) {
  const dpr = window.devicePixelRatio || 1
  canvas.width = width * dpr
  canvas.height = height * dpr
  canvas.style.width = width + 'px'
  canvas.style.height = height + 'px'
  const ctx = canvas.getContext('2d')
  ctx.scale(dpr, dpr)
  return ctx
}

export function drawText(ctx, text, x, y, options = {}) {
  ctx.fillStyle = options.color || '#202124'
  ctx.font = options.font || '12px sans-serif'
  ctx.textAlign = options.align || 'center'
  ctx.textBaseline = options.baseline || 'middle'
  ctx.fillText(text, x, y)
}
