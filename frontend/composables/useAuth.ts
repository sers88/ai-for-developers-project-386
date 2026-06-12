interface LoginData {
  email: string
  password: string
}

interface RegisterData {
  email: string
  password: string
}

interface AuthResponse {
  accessToken: string
  refreshToken: string
  user: { id: string; email: string }
}

interface TokenResponse {
  accessToken: string
}

export const useAuth = () => {
  const config = useRuntimeConfig()
  const router = useRouter()
  const store = useAuthStore()

  async function register(data: RegisterData): Promise<AuthResponse> {
    const response = await $fetch<AuthResponse>(`${config.public.apiBase}/api/auth/register`, {
      method: "POST",
      body: data,
    })
    store.setTokens(response.accessToken, response.refreshToken)
    store.setUser(response.user)
    return response
  }

  async function login(data: LoginData): Promise<AuthResponse> {
    const response = await $fetch<AuthResponse>(`${config.public.apiBase}/api/auth/login`, {
      method: "POST",
      body: data,
    })
    store.setTokens(response.accessToken, response.refreshToken)
    store.setUser(response.user)
    return response
  }

  async function refreshAccessToken(): Promise<string> {
    const token = store.refreshToken
    if (!token) throw new Error("No refresh token")

    const response = await $fetch<TokenResponse>(`${config.public.apiBase}/api/auth/refresh`, {
      method: "POST",
      body: { refreshToken: token },
    })
    store.accessToken = response.accessToken
    return response.accessToken
  }

  function logout() {
    store.clearAuth()
    router.push("/login")
  }

  return { register, login, refreshAccessToken, logout }
}
