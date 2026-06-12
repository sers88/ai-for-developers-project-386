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
  }

  function setUser(u: User) {
    user.value = u
  }

  function clearAuth() {
    accessToken.value = null
    refreshToken.value = null
    user.value = null
  }

  return { accessToken, refreshToken, user, isAuthenticated, setTokens, setUser, clearAuth }
})
