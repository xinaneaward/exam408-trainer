import { ref, computed, onMounted, onUnmounted } from 'vue'

const lightPalette = {
  bg: '#ffffff',
  cardBg: '#f8f9fa',
  text: '#202124',
  textSecondary: '#5f6368',
  primary: '#1a73e8',
  primaryLight: '#e8f0fe',
  success: '#0d904f',
  successLight: '#c8e6c9',
  danger: '#d93025',
  warning: '#f9ab00',
  accent: '#9334e6',
  edge: '#5f6368',
  border: '#e5e7eb'
}

const darkPalette = {
  bg: '#0f172a',
  cardBg: '#1e293b',
  text: '#f1f5f9',
  textSecondary: '#94a3b8',
  primary: '#60a5fa',
  primaryLight: '#1e293b',
  success: '#34d399',
  successLight: '#064e3b',
  danger: '#f87171',
  warning: '#fbbf24',
  accent: '#c084fc',
  edge: '#94a3b8',
  border: '#334155'
}

export function useVisualizationTheme() {
  const isDark = ref(false)

  const update = () => {
    isDark.value = document.body.classList.contains('dark-mode')
  }

  let observer = null
  onMounted(() => {
    update()
    observer = new MutationObserver(update)
    observer.observe(document.body, { attributes: true, attributeFilter: ['class'] })
  })

  onUnmounted(() => {
    if (observer) observer.disconnect()
  })

  const colors = computed(() => (isDark.value ? darkPalette : lightPalette))

  return { isDark, colors }
}
