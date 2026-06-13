<script setup lang="ts">
import { useApiClient } from "~/api/client"

definePageMeta({
  middleware: ["auth"],
})

const store = useAuthStore()
const api = useApiClient()
const { loadBookings, cancelBooking } = useBookings()

const { data: me } = await api.GET("/api/me")

const loading = ref(true)
const generalError = ref<string | null>(null)
const bookings = ref<Awaited<ReturnType<typeof loadBookings>>>([])
const filter = ref<"upcoming" | "past">("upcoming")

async function load() {
  loading.value = true
  generalError.value = null
  try {
    bookings.value = await loadBookings(filter.value)
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to load bookings"
  } finally {
    loading.value = false
  }
}

async function handleCancel(id: string) {
  if (!confirm("Cancel this booking? This will notify the guest.")) return
  try {
    await cancelBooking(id)
    await load()
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to cancel booking"
  }
}

async function logout() {
  store.clearAuth()
  await navigateTo("/login")
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  })
}

watch(filter, () => load())

await load()
</script>

<template>
  <div class="page">
    <div class="header">
      <h1>Dashboard</h1>
      <button type="button" class="btn-logout" @click="logout">Logout</button>
    </div>
    <p v-if="me" class="welcome">Welcome, {{ me.email }}</p>

    <div class="tabs">
      <button
        type="button"
        class="tab"
        :class="{ active: filter === 'upcoming' }"
        @click="filter = 'upcoming'"
      >
        Upcoming
      </button>
      <button
        type="button"
        class="tab"
        :class="{ active: filter === 'past' }"
        @click="filter = 'past'"
      >
        Past
      </button>
    </div>

    <p v-if="generalError" class="error">{{ generalError }}</p>
    <div v-if="loading" class="loading">Loading...</div>

    <div v-else-if="bookings.length === 0" class="empty">
      <p v-if="filter === 'upcoming'">No upcoming bookings.</p>
      <p v-else>No past bookings.</p>
    </div>

    <div v-else class="list">
      <div v-for="b in bookings" :key="b.id" class="card">
        <div class="card-body">
          <h3>{{ b.eventTitle }}</h3>
          <div class="meta">
            <span>{{ b.guestName }}</span>
            <span class="email">{{ b.guestEmail }}</span>
          </div>
          <p class="date">{{ formatDate(b.startTime) }}</p>
          <p v-if="b.notes" class="notes">{{ b.notes }}</p>
          <span v-if="b.status === 'CANCELLED'" class="badge cancelled">Cancelled</span>
        </div>
        <div v-if="b.status === 'CONFIRMED'" class="card-actions">
          <button type="button" class="btn-cancel" @click="handleCancel(b.id)">Cancel</button>
        </div>
      </div>
    </div>

    <div class="nav-links">
      <NuxtLink to="/event-types" class="nav-link">Event Types</NuxtLink>
      <NuxtLink to="/schedules" class="nav-link">Schedules</NuxtLink>
      <NuxtLink to="/settings" class="nav-link">Settings</NuxtLink>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; padding: 2rem 1rem; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem; }
.header h1 { margin: 0; }
.btn-logout { background: none; color: #666; border: 1px solid #ddd; padding: 0.4rem 1rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; }
.btn-logout:hover { background: #f5f5f5; }
.welcome { color: #666; margin: 0 0 1.5rem; }
.tabs { display: flex; gap: 0.5rem; margin-bottom: 1.5rem; }
.tab { background: none; border: 1px solid #ddd; padding: 0.4rem 1.25rem; border-radius: 6px; cursor: pointer; font-size: 0.9rem; color: #666; }
.tab.active { background: #0070f3; color: #fff; border-color: #0070f3; }
.loading { text-align: center; color: #666; padding: 2rem; }
.error { color: #cc0000; margin-bottom: 1rem; }
.empty { text-align: center; color: #888; padding: 3rem 0; }
.list { display: flex; flex-direction: column; gap: 1rem; }
.card { border: 1px solid #e0e0e0; border-radius: 8px; padding: 1.25rem; display: flex; justify-content: space-between; align-items: flex-start; }
.card-body { flex: 1; }
.card-body h3 { margin: 0 0 0.25rem; }
.meta { display: flex; gap: 1rem; color: #777; font-size: 0.85rem; margin: 0.25rem 0; }
.email { color: #999; }
.date { color: #333; font-size: 0.9rem; margin: 0.25rem 0; }
.notes { color: #666; font-size: 0.85rem; font-style: italic; margin: 0.25rem 0; }
.badge { display: inline-block; padding: 0.15rem 0.6rem; border-radius: 4px; font-size: 0.75rem; font-weight: 600; }
.badge.cancelled { background: #fee; color: #c00; }
.card-actions { display: flex; gap: 0.5rem; flex-shrink: 0; margin-left: 1rem; }
.btn-cancel { background: none; color: #cc0000; border: 1px solid #cc0000; padding: 0.35rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.85rem; }
.btn-cancel:hover { background: #cc0000; color: #fff; }
.nav-links { display: flex; gap: 1.5rem; margin-top: 2rem; justify-content: center; }
.nav-link { color: #0070f3; text-decoration: none; font-size: 0.9rem; }
.nav-link:hover { text-decoration: underline; }
</style>
