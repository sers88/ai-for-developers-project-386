<script setup lang="ts">
definePageMeta({
  layout: "public",
  middleware: [],
})

const route = useRoute()
const { cancelBooking } = useBookings()

const token = route.query.token as string | undefined
const id = route.query.id as string | undefined

const cancelling = ref(false)
const cancelled = ref(false)
const error = ref<string | null>(null)

async function handleCancel() {
  if (!id || !token) {
    error.value = "Invalid cancellation link"
    return
  }
  cancelling.value = true
  error.value = null
  try {
    await cancelBooking(id, token)
    cancelled.value = true
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Failed to cancel booking"
  } finally {
    cancelling.value = false
  }
}
</script>

<template>
  <div class="cancel-page">
    <div class="cancel-card">
      <div v-if="cancelled" class="cancelled-state">
        <div class="check-icon">
          <svg width="56" height="56" viewBox="0 0 24 24" fill="none" stroke="#22c55e" stroke-width="2">
            <circle cx="12" cy="12" r="10" />
            <path d="M9 12l2 2 4-4" />
          </svg>
        </div>
        <h1>Booking Cancelled</h1>
        <p class="info">Your booking has been successfully cancelled.</p>
        <NuxtLink to="/" class="btn-done">Done</NuxtLink>
      </div>

      <div v-else>
        <h1>Cancel Booking</h1>
        <p class="confirm-text">
          Are you sure you want to cancel this booking? This action cannot be undone.
        </p>

        <p v-if="error" class="error">{{ error }}</p>

        <div class="actions">
          <button
            type="button"
            class="btn-cancel"
            :disabled="cancelling || !id || !token"
            @click="handleCancel"
          >
            <span v-if="cancelling">Cancelling...</span>
            <span v-else>Yes, Cancel Booking</span>
          </button>
          <NuxtLink to="/" class="btn-keep">Keep Booking</NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cancel-page { max-width: 500px; margin: 0 auto; padding: 3rem 1rem; text-align: center; }
.cancel-card { background: #fff; border: 1px solid #e0e0e0; border-radius: 12px; padding: 2.5rem 2rem; }
.cancel-card h1 { margin-bottom: 1rem; }
.confirm-text { color: #555; margin-bottom: 2rem; }
.error { color: #cc0000; margin-bottom: 1rem; }
.actions { display: flex; flex-direction: column; gap: 0.75rem; align-items: center; }
.btn-cancel { background: #dc2626; color: #fff; border: none; padding: 0.65rem 2rem; border-radius: 6px; cursor: pointer; font-size: 0.95rem; width: 100%; }
.btn-cancel:hover:not(:disabled) { background: #b91c1c; }
.btn-cancel:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-keep { color: #666; text-decoration: none; font-size: 0.9rem; }
.btn-keep:hover { text-decoration: underline; }
.cancelled-state { }
.cancelled-state .check-icon { margin-bottom: 1rem; }
.cancelled-state .info { color: #666; margin-bottom: 1.5rem; }
.btn-done { display: inline-block; background: #0070f3; color: #fff; text-decoration: none; padding: 0.6rem 2rem; border-radius: 6px; font-size: 0.95rem; }
.btn-done:hover { background: #0051cc; }
</style>
