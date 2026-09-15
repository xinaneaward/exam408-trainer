import { reactive } from 'vue'

export const vizLive = reactive({
  desc: '',
  line: -1,
  step: 0,
  active: false
})

export function setLiveDesc(text) {
  vizLive.desc = text ?? ''
  vizLive.active = true
}

export function setLiveLine(line) {
  vizLive.line = line ?? -1
  vizLive.active = true
}

export function setLiveStep(step) {
  vizLive.step = step ?? 0
}

export function clearLive() {
  vizLive.desc = ''
  vizLive.line = -1
  vizLive.step = 0
  vizLive.active = false
}