import { useApiClient } from "~/api/client"

export const useAuth = () => {
  const router = useRouter()
  const store = useAuthStore()
  const api = useApiClient()

  async function register(data: { email: string; password: string }) {
    const { data: response, error } = await api.POST("/api/auth/register", { body: data })
    if (error || !response) throw new Error(error?.message || "Registration failed")
    store.setTokens(response.accessToken, response.refreshToken)
    store.setUser(response.user)
    return response
  }

  async function login(data: { email: string; password: string }) {
    const { data: response, error } = await api.POST("/api/auth/login", { body: data })
    if (error || !response) throw new Error(error?.message || "Login failed")
    store.setTokens(response.accessToken, response.refreshToken)
    store.setUser(response.user)
    return response
  }

  async function refreshAccessToken() {
    const token = store.refreshToken
    if (!token) throw new Error("No refresh token")

    const { data: response, error } = await api.POST("/api/auth/refresh", {
      body: { refreshToken: token },
    })
    if (error || !response) throw new Error(error?.message || "Token refresh failed")
    store.accessToken = response.accessToken
    return response.accessToken
  }

  function logout() {
    store.clearAuth()
    router.push("/login")
  }

  return { register, login, refreshAccessToken, logout }
}
