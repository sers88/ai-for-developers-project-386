export const useGoogleAuth = () => {
  const config = useRuntimeConfig()
  const apiBase = config.public.apiBase as string

  function redirectToGoogle() {
    window.location.href = `${apiBase}/api/auth/oauth2/google`
  }

  return { redirectToGoogle }
}
