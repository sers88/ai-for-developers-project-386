<script setup lang="ts">
definePageMeta({
  middleware: ["auth"],
})

const route = useRoute()
const router = useRouter()
const id = route.params.id as string
const { getEventType, updateEventType, deleteEventType } = useEventTypes()
const { load: loadSchedules, schedules, loading: schedulesLoading } = useSchedulesForSelect()
const { errors, isSubmitting, generalError, handleSubmit, initFromEventType } = useEventTypeForm()

const loading = ref(true)
const error = ref<string | null>(null)
const editing = ref(false)
const eventType = ref<Awaited<ReturnType<typeof getEventType>> | null>(null)

async function load() {
  loading.value = true
  error.value = null
  try {
    eventType.value = await getEventType(id)
    initFromEventType(eventType.value)
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Failed to load event type"
  } finally {
    loading.value = false
  }
}

await loadSchedules()
await load()

function copyBookingUrl(url: string) {
  navigator.clipboard.writeText(url).then(() => {
    alert("Booking link copied!")
  }).catch(() => {
    prompt("Copy this link:", url)
  })
}

async function handleDelete() {
  if (!confirm("Delete this event type?")) return
  try {
    await deleteEventType(id)
    router.push("/event-types")
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Failed to delete"
  }
}

const submit = handleSubmit(async (values) => {
  generalError.value = null
  try {
    await updateEventType(id, {
      title: values.title,
      description: values.description || undefined,
      duration: values.duration,
      scheduleId: values.scheduleId || undefined,
      bufferBefore: values.bufferBefore,
      bufferAfter: values.bufferAfter,
    })
    editing.value = false
    await load()
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to update event type"
  }
})
</script>

<template>
  <div class="page">
    <div v-if="loading" class="loading">Loading...</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <div v-else-if="eventType && !editing" class="detail">
      <div class="header">
        <h1>{{ eventType.title }}</h1>
        <div class="header-actions">
          <button class="btn-edit" @click="editing = true">Edit</button>
          <button class="btn-delete" @click="handleDelete">Delete</button>
        </div>
      </div>

      <p v-if="eventType.description" class="description">{{ eventType.description }}</p>

      <div class="info-grid">
        <div class="info-item">
          <span class="label">Duration</span>
          <span class="value">{{ eventType.duration }} min</span>
        </div>
        <div class="info-item">
          <span class="label">Slug</span>
          <span class="value mono">/{{ eventType.slug }}</span>
        </div>
        <div class="info-item">
          <span class="label">Buffer Before</span>
          <span class="value">{{ eventType.bufferBefore }} min</span>
        </div>
        <div class="info-item">
          <span class="label">Buffer After</span>
          <span class="value">{{ eventType.bufferAfter }} min</span>
        </div>
        <div v-if="eventType.scheduleName" class="info-item">
          <span class="label">Schedule</span>
          <span class="value">{{ eventType.scheduleName }}</span>
        </div>
        <div class="info-item">
          <span class="label">Created</span>
          <span class="value">{{ new Date(eventType.createdAt).toLocaleDateString() }}</span>
        </div>
      </div>

      <div class="booking-section">
        <p class="booking-label">Booking Link</p>
        <div class="booking-link">
          <code>{{ eventType.bookingUrl }}</code>
          <button class="btn-copy" @click="copyBookingUrl(eventType.bookingUrl)">Copy</button>
        </div>
      </div>

      <NuxtLink to="/event-types" class="btn-back">&larr; Back to list</NuxtLink>
    </div>

    <div v-else-if="eventType && editing" class="edit-form">
      <h1>Edit {{ eventType.title }}</h1>

      <form @submit.prevent="submit">
        <div class="field">
          <label for="title">Title</label>
          <input id="title" name="title" type="text">
          <p v-if="errors.title" class="field-error">{{ errors.title }}</p>
        </div>

        <div class="field">
          <label for="description">Description</label>
          <input id="description" name="description" type="text">
        </div>

        <div class="field">
          <label for="duration">Duration (minutes)</label>
          <input id="duration" name="duration" type="number" min="5">
          <p v-if="errors.duration" class="field-error">{{ errors.duration }}</p>
        </div>

        <div class="field">
          <label for="scheduleId">Schedule</label>
          <select id="scheduleId" name="scheduleId" :disabled="schedulesLoading">
            <option value="">None</option>
            <option v-for="s in schedules" :key="s.id" :value="s.id">
              {{ s.name }} ({{ s.timezone }})
            </option>
          </select>
        </div>

        <div class="field-row">
          <div class="field">
            <label for="bufferBefore">Buffer Before (min)</label>
            <input id="bufferBefore" name="bufferBefore" type="number" min="0">
            <p v-if="errors.bufferBefore" class="field-error">{{ errors.bufferBefore }}</p>
          </div>
          <div class="field">
            <label for="bufferAfter">Buffer After (min)</label>
            <input id="bufferAfter" name="bufferAfter" type="number" min="0">
            <p v-if="errors.bufferAfter" class="field-error">{{ errors.bufferAfter }}</p>
          </div>
        </div>

        <p v-if="generalError" class="error">{{ generalError }}</p>

        <div class="actions">
          <button type="button" class="btn-cancel" @click="editing = false">Cancel</button>
          <button type="submit" class="btn-save" :disabled="isSubmitting">
            {{ isSubmitting ? "Saving..." : "Save" }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.page { max-width: 640px; margin: 0 auto; padding: 2rem 1rem; }
.loading { text-align: center; color: #666; padding: 2rem; }
.error { color: #cc0000; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.header h1 { margin: 0; }
.header-actions { display: flex; gap: 0.5rem; }
.btn-edit { background: #0070f3; color: #fff; border: none; padding: 0.4rem 1rem; border-radius: 6px; cursor: pointer; }
.btn-edit:hover { background: #0051cc; }
.btn-delete { background: none; color: #cc0000; border: 1px solid #cc0000; padding: 0.4rem 1rem; border-radius: 6px; cursor: pointer; }
.btn-delete:hover { background: #cc0000; color: #fff; }
.description { color: #555; margin-bottom: 1.5rem; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-bottom: 1.5rem; }
.info-item .label { display: block; font-weight: 600; font-size: 0.85rem; color: #777; }
.info-item .value { font-size: 0.95rem; }
.mono { font-family: monospace; font-size: 0.85rem; }
.booking-section { background: #f5f5f5; padding: 1rem; border-radius: 6px; margin-bottom: 1.5rem; }
.booking-label { font-weight: 600; margin: 0 0 0.5rem; }
.booking-link { display: flex; gap: 0.5rem; align-items: center; }
.booking-link code { background: #e8e8e8; padding: 0.25rem 0.5rem; border-radius: 4px; font-size: 0.85rem; word-break: break-all; }
.btn-copy { background: #0070f3; color: #fff; border: none; padding: 0.35rem 0.75rem; border-radius: 4px; cursor: pointer; font-size: 0.85rem; }
.btn-copy:hover { background: #0051cc; }
.btn-back { color: #0070f3; text-decoration: none; font-size: 0.9rem; }
.btn-back:hover { text-decoration: underline; }
.field { margin-bottom: 1rem; }
.field label { display: block; font-weight: 600; margin-bottom: 0.25rem; font-size: 0.9rem; }
.field input, .field select { width: 100%; padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; font-size: 0.95rem; box-sizing: border-box; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.field-error { color: #cc0000; font-size: 0.8rem; margin: 0.25rem 0 0; }
.actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }
.btn-cancel { background: none; color: #555; border: 1px solid #ccc; padding: 0.5rem 1rem; border-radius: 6px; cursor: pointer; font-size: 0.95rem; }
.btn-cancel:hover { background: #f5f5f5; }
.btn-save { background: #0070f3; color: #fff; border: none; padding: 0.5rem 1.5rem; border-radius: 6px; font-size: 0.95rem; cursor: pointer; }
.btn-save:hover:not(:disabled) { background: #0051cc; }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
