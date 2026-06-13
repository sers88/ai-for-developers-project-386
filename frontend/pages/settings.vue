<script setup lang="ts">
definePageMeta({

  middleware: ["auth"],
})

const route = useRoute()
const { getConnectionStatus } = useCalendar()
const { redirectToGoogleCalendar } = useCalendarAuth()

const loading = ref(true)
const generalError = ref<string | null>(null)
const connected = ref(false)
const calendarEmail = ref<string | null>(null)
const justConnected = route.query.calendar === "connected"

async function loadStatus() {
  loading.value = true
  generalError.value = null
  try {
    const status = await getConnectionStatus()
    connected.value = status.connected
    calendarEmail.value = status.email ?? null
  } catch (e) {
    generalError.value =
      e instanceof Error ? e.message : "Failed to load calendar status"
  } finally {
    loading.value = false
  }
}

await loadStatus()
</script>

<template>
  <div class="settings">
    <h1>Settings</h1>

    <div class="section">
      <h2>Google Calendar</h2>

      <p v-if="justConnected" class="success">
        Google Calendar connected successfully!
      </p>

      <div v-if="loading" class="loading">Loading...</div>

      <div v-else-if="generalError" class="error">
        {{ generalError }}
      </div>

      <div v-else>
        <div v-if="connected" class="status connected">
          <p><strong>Status:</strong> Connected</p>
          <p><strong>Email:</strong> {{ calendarEmail }}</p>
        </div>

        <div v-else class="status disconnected">
          <p><strong>Status:</strong> Not connected</p>
        </div>

        <button
          v-if="!connected"
          class="connect-btn"
          :disabled="loading"
          @click="redirectToGoogleCalendar"
        >
          Connect Google Calendar
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings {
  max-width: 600px;
  margin: 0 auto;
  padding: 2rem;
}

.section {
  margin-top: 2rem;
  padding: 1.5rem;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
}

.section h2 {
  margin-top: 0;
}

.status {
  margin-bottom: 1rem;
}

.status p {
  margin: 0.25rem 0;
}

.connected {
  color: #2e7d32;
}

.disconnected {
  color: #757575;
}

.success {
  color: #2e7d32;
  font-weight: 500;
}

.loading {
  color: #757575;
}

.error {
  color: #d32f2f;
}

.connect-btn {
  padding: 0.75rem 1.5rem;
  background-color: #4285f4;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
}

.connect-btn:hover:not(:disabled) {
  background-color: #3367d6;
}

.connect-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>

