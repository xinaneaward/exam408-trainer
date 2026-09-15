import { ref } from 'vue'
import { clearLive, setLiveDesc, setLiveLine, setLiveStep } from './vizLive.js'

export function usePlayback(defaultSpeed = 50) {
  const running = ref(false)
  const paused = ref(false)
  const speed = ref(defaultSpeed)
  const abort = ref(false)
  const stepMode = ref(false)
  const stepRequested = ref(false)
  const currentStep = ref(0)
  const desc = ref('')
  const line = ref(-1)

  const setDesc = (text) => {
    desc.value = text ?? ''
    setLiveDesc(desc.value)
  }
  const setLine = (idx) => {
    line.value = idx ?? -1
    setLiveLine(line.value)
  }
  const clearDescs = () => {
    desc.value = ''
    line.value = -1
    clearLive()
  }

  const start = () => {
    running.value = true
    abort.value = false
    paused.value = false
    stepMode.value = false
    stepRequested.value = false
    currentStep.value = 0
    setLiveStep(0)
    setLiveDesc('')
    setLiveLine(-1)
  }

  const startStepMode = () => {
    running.value = true
    abort.value = false
    paused.value = false
    stepMode.value = true
    currentStep.value = 0
    stepRequested.value = false
    setLiveStep(0)
    setLiveDesc('')
    setLiveLine(-1)
  }

  const exitStepMode = () => {
    stepMode.value = false
    stepRequested.value = true
    paused.value = false
  }

  const switchToStepMode = () => {
    if (stepMode.value) {
      exitStepMode()
      return
    }
    if (!running.value) {
      startStepMode()
      return
    }
    stepMode.value = true
    stepRequested.value = false
    paused.value = false
  }

  const pause = () => { paused.value = true }
  const resume = () => { paused.value = false }

  const stop = () => {
    running.value = false
    abort.value = true
    paused.value = false
    stepMode.value = false
  }

  const resetPlayback = () => {
    running.value = false
    paused.value = false
    abort.value = false
    stepMode.value = false
    currentStep.value = 0
    clearDescs()
  }

  const step = () => {
    stepRequested.value = true
  }

  const sleep = (ms) => new Promise(resolve => {
    if (abort.value) {
      resolve(false)
      return
    }

    const delay = ms ?? speed.value
    let timeoutId = null

    const tick = () => {
      currentStep.value++
      setLiveStep(currentStep.value)
      resolve(true)
    }

    const check = () => {
      if (abort.value) {
        if (timeoutId) clearTimeout(timeoutId)
        resolve(false)
        return
      }
      if (paused.value) {
        timeoutId = setTimeout(check, 50)
        return
      }
      if (stepMode.value) {
        if (stepRequested.value) {
          stepRequested.value = false
          tick()
        } else {
          timeoutId = setTimeout(check, 50)
        }
      } else {
        timeoutId = setTimeout(() => {
          if (abort.value) resolve(false)
          else if (stepMode.value) {
            check()
          } else {
            tick()
          }
        }, delay)
      }
    }

    check()
  })

  return {
    running,
    paused,
    speed,
    abort,
    stepMode,
    currentStep,
    desc,
    line,
    setDesc,
    setLine,
    start,
    startStepMode,
    switchToStepMode,
    exitStepMode,
    pause,
    resume,
    stop,
    resetPlayback,
    step,
    sleep
  }
}