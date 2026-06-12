<script setup lang="ts">
definePageMeta({
  middleware: ["auth"],
})

const config = useRuntimeConfig()
const store = useAuthStore()

interface MeResponse {
  id: string
  email: string
}

const { data } = useFetch<MeResponse>(`${config.public.apiBase}/api/me`, {
  headers: {
    Authorization: `Bearer ${store.accessToken}`,
  },
})

async function logout() {
  store.clearAuth()
  await navigateTo("/login")
}
</script>

<template>
  <div class="page">
    <h1>Dashboard</h1>
    <p v-if="data">Welcome, {{ data.email }}</p>
    <button type="button" @click="logout">Logout</button>
  </div>
</template>
