<script setup lang="ts">
definePageMeta({
  layout: "auth",
})

const route = useRoute()
const router = useRouter()
const store = useAuthStore()

const accessToken = route.query.accessToken as string | undefined
const refreshToken = route.query.refreshToken as string | undefined
const email = route.query.email as string | undefined

if (accessToken && refreshToken && email) {
  store.setTokens(accessToken, refreshToken)
  store.setUser({ id: "pending", email })
  router.replace("/dashboard")
}
</script>

<template>
  <div class="flex flex-col items-center gap-4 py-8">
    <UIcon
      name="i-lucide-loader-circle"
      class="size-8 animate-spin text-primary"
      data-testid="callback-spinner"
    />
    <p class="text-sm text-muted">Completing sign in...</p>
  </div>
</template>
