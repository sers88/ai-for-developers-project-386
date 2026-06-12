<script setup lang="ts">
import { useApiClient } from "~/api/client"

definePageMeta({
  middleware: ["auth"],
})

const store = useAuthStore()
const api = useApiClient()

const { data: me } = await api.GET("/api/me")

async function logout() {
  store.clearAuth()
  await navigateTo("/login")
}
</script>

<template>
  <div class="page">
    <h1>Dashboard</h1>
    <p v-if="me">Welcome, {{ me.email }}</p>
    <button type="button" @click="logout">Logout</button>
  </div>
</template>
