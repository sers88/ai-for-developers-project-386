 <script setup lang="ts">
definePageMeta({
  layout: false,
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
  <div class="page">
    <p>Completing sign in...</p>
  </div>
</template>
