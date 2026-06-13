import type { components } from "~/api/generated/schema"

type User = components["schemas"]["UserResponse"]

export const useAuthStore = defineStore("auth", () => {
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  function setTokens(access: string, refresh: string) {
    accessToken.value = access
    refreshToken.value = refresh
    if (import.meta.client) {
      localStorage.setItem("accessToken", access)
      localStorage.setItem("refreshToken", refresh)
    }
  }

  function setUser(u: User) {
    user.value = u
    if (import.meta.client) {
      localStorage.setItem("user", JSON.stringify(u))
    }
  }

  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
    if (import.meta.client) {
      localStorage.removeItem("accessToken")
      localStorage.removeItem("refreshToken")
      localStorage.removeItem("user")
    }
  }

  if (import.meta.client) {
    accessToken.value = localStorage.getItem("accessToken")
    refreshToken.value = localStorage.getItem("refreshToken")
    const stored = localStorage.getItem("user")
    if (stored) {
      try {
        user.value = JSON.parse(stored) as User
      } catch {
        localStorage.removeItem("user")
      }
    }
  }

  return { accessToken, refreshToken, user, isAuthenticated, setTokens, setUser, clearAuth }
})
