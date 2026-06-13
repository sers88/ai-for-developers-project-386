<script setup lang="ts">
definePageMeta({
  middleware: ["auth"],
})

const { loadEventTypes, deleteEventType } = useEventTypes()

const loading = ref(true)
const generalError = ref<string | null>(null)
const eventTypes = ref<Awaited<ReturnType<typeof loadEventTypes>>>([])

async function load() {
  loading.value = true
  generalError.value = null
  try {
    eventTypes.value = await loadEventTypes()
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to load event types"
  } finally {
    loading.value = false
  }
}

async function handleDelete(id: string) {
  if (!confirm("Delete this event type?")) return
  try {
    await deleteEventType(id)
    await load()
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to delete"
  }
}

function copyBookingUrl(url: string) {
  navigator.clipboard.writeText(url).then(() => {
    alert("Booking link copied!")
  }).catch(() => {
    prompt("Copy this link:", url)
  })
}

await load()
</script>

<template>
  <div class="page">
    <div class="header">
      <h1>Event Types</h1>
      <NuxtLink to="/event-types/create" class="btn-create">+ New Event Type</NuxtLink>
    </div>

    <p v-if="generalError" class="error">{{ generalError }}</p>
    <div v-if="loading" class="loading">Loading...</div>

    <div v-else-if="eventTypes.length === 0" class="empty">
      <p>No event types yet. Create your first one!</p>
    </div>

    <div v-else class="grid">
      <div v-for="et in eventTypes" :key="et.id" class="card">
        <div class="card-body">
          <h3>{{ et.title }}</h3>
          <p v-if="et.description" class="description">{{ et.description }}</p>
          <div class="meta">
            <span>{{ et.duration }} min</span>
            <span v-if="et.bufferBefore || et.bufferAfter" class="buffers">
              Buffer: {{ et.bufferBefore }}m / {{ et.bufferAfter }}m
            </span>
          </div>
          <p class="slug">/{{ et.slug }}</p>
        </div>
        <div class="card-actions">
          <NuxtLink :to="`/event-types/${et.id}`" class="btn-link">Details</NuxtLink>
          <button class="btn-copy" @click="copyBookingUrl(et.bookingUrl)">Copy Link</button>
          <button class="btn-delete" @click="handleDelete(et.id)">Delete</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; padding: 2rem 1rem; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
.header h1 { margin: 0; }
.btn-create { background: #0070f3; color: #fff; border: none; padding: 0.5rem 1.25rem; border-radius: 6px; text-decoration: none; font-size: 0.95rem; }
.btn-create:hover { background: #0051cc; }
.loading { text-align: center; color: #666; padding: 2rem; }
.error { color: #cc0000; margin-bottom: 1rem; }
.empty { text-align: center; color: #888; padding: 3rem 0; }
.grid { display: flex; flex-direction: column; gap: 1rem; }
.card { border: 1px solid #e0e0e0; border-radius: 8px; padding: 1.25rem; display: flex; justify-content: space-between; align-items: flex-start; }
.card-body { flex: 1; }
.card-body h3 { margin: 0 0 0.25rem; }
.description { color: #555; font-size: 0.9rem; margin: 0.25rem 0; }
.meta { display: flex; gap: 1rem; color: #777; font-size: 0.85rem; margin: 0.25rem 0; }
.slug { color: #999; font-size: 0.8rem; font-family: monospace; margin: 0.25rem 0 0; }
.card-actions { display: flex; gap: 0.5rem; flex-shrink: 0; margin-left: 1rem; }
.btn-link { color: #0070f3; text-decoration: none; padding: 0.35rem 0.75rem; font-size: 0.85rem; border: 1px solid #0070f3; border-radius: 4px; }
.btn-link:hover { background: #0070f3; color: #fff; }
.btn-copy { background: none; color: #333; border: 1px solid #ccc; padding: 0.35rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.85rem; }
.btn-copy:hover { background: #f5f5f5; }
.btn-delete { background: none; color: #cc0000; border: 1px solid #cc0000; padding: 0.35rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.85rem; }
.btn-delete:hover { background: #cc0000; color: #fff; }
</style>
