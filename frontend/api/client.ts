import createClient from "openapi-fetch"
import type { paths } from "./generated/schema"

export const useApiClient = () => {
  const config = useRuntimeConfig()
  const store = useAuthStore()

  return createClient<paths>({
    baseUrl: config.public.apiBase as string,
    fetch: (request) => {
      if (store.accessToken) {
        request.headers.set("Authorization", `Bearer ${store.accessToken}`)
      }
      return globalThis.fetch(request)
    },
  })
}
