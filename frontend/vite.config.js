import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import fs from 'node:fs'
import path from 'node:path'

const outDir = path.resolve(__dirname, '../backend/src/main/resources/static')

function cleanOldJs() {
  const assetsDir = path.join(outDir, 'assets')
  const indexHtml = path.join(outDir, 'index.html')
  if (!fs.existsSync(assetsDir)) return
  const files = fs.readdirSync(assetsDir).filter(f => f.endsWith('.js') || f.endsWith('.css'))
  if (files.length > 30) {
    fs.rmSync(assetsDir, { recursive: true, force: true })
    if (fs.existsSync(indexHtml)) fs.rmSync(indexHtml, { force: true })
  }
}

cleanOldJs()

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8081',
      '/images': 'http://localhost:8081'
    }
  },
  build: {
    outDir,
    emptyOutDir: false
  }
})
