<script setup lang="ts">
import type { components } from "~/api/generated/schema"

definePageMeta({
  layout: "public",
})

const route = useRoute()
const { getPublicEventType } = usePublicBooking()

const userId = route.params.userId as string
const slug = route.params.slug as string

const eventType = ref<PublicEventTypeResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

type PublicEventTypeResponse = components["schemas"]["PublicEventTypeResponse"]

try {
  eventType.value = await getPublicEventType(userId, slug)
} catch (e) {
  error.value = e instanceof Error ? e.message : "Failed to load event type"
} finally {
  loading.value = false
}
</script>

<template>
  <div class="success-page">
    <div v-if="loading" class="loading">Loading...</div>

    <div v-else class="success-card">
      <div class="check-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <path d="M9 12l2 2 4-4" />
        </svg>
      </div>

      <h1 data-testid="booking-confirmed">Booking Confirmed!</h1>

      <div v-if="eventType" class="booking-details">
        <h2>{{ eventType.title }}</h2>
        <p v-if="eventType.description" class="description">{{ eventType.description }}</p>
        <p class="duration">{{ eventType.duration }} minutes</p>
        <p v-if="eventType.ownerName" class="owner">with {{ eventType.ownerName }}</p>
        <p class="timezone">Timezone: {{ eventType.timezone }}</p>
      </div>

      <p class="info">A calendar invitation has been sent to your email.</p>

      <NuxtLink to="/" class="btn-done">Done</NuxtLink>
    </div>
  </div>
</template>

<style scoped>
.success-page { max-width: 500px; margin: 0 auto; padding: 3rem 1rem; text-align: center; }
.loading { color: #666; }
.success-card { background: #fff; border: 1px solid #e0e0e0; border-radius: 12px; padding: 2.5rem 2rem; }
.check-icon { margin-bottom: 1rem; }
.success-card h1 { margin-bottom: 1.5rem; }
.booking-details { text-align: left; background: #f9f9f9; border-radius: 8px; padding: 1rem 1.5rem; margin-bottom: 1.5rem; }
.booking-details h2 { margin-bottom: 0.5rem; }
.description { color: #555; margin-bottom: 0.5rem; }
.duration { font-weight: 600; color: #0070f3; }
.owner { color: #555; }
.timezone { color: #999; font-size: 0.85rem; }
.info { color: #666; margin-bottom: 1.5rem; }
.btn-done { display: inline-block; background: #0070f3; color: #fff; text-decoration: none; padding: 0.6rem 2rem; border-radius: 6px; font-size: 0.95rem; }
.btn-done:hover { background: #0051cc; }
</style>
