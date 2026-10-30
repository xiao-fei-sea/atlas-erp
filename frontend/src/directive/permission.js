import { useUserStore } from '@/store/user'

export default {
  mounted(el, binding) {
    const store = useUserStore()
    const value = binding.value
    const codes = Array.isArray(value) ? value : [value]
    const allowed = codes.some((code) => store.hasPermission(code))
    if (!allowed && el.parentNode) {
      el.parentNode.removeChild(el)
    }
  }
}
