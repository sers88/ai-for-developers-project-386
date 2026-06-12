export const useAuthStore = defineStore("auth", () => {
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const user = ref<{ id: string; email: string } | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
  }

  function setUser(u: { id: string; email: string }) {
    user.value = u
  }

  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
  }

  return { accessToken, refreshToken, user, isAuthenticated, setTokens, setUser, clearAuth }
})
