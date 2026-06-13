<script setup lang="ts">
definePageMeta({
  middleware: ["auth"],
})

const router = useRouter()
const { createEventType } = useEventTypes()
const { load: loadSchedules, schedules, loading: schedulesLoading } = useSchedulesForSelect()
const { errors, isSubmitting, generalError, handleSubmit } = useEventTypeForm()

await loadSchedules()

const submit = handleSubmit(async (values) => {
  generalError.value = null
  try {
    await createEventType({
      title: values.title,
      description: values.description || undefined,
      duration: values.duration,
      scheduleId: values.scheduleId || undefined,
      bufferBefore: values.bufferBefore,
      bufferAfter: values.bufferAfter,
    })
    router.push("/event-types")
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to create event type"
  }
})
</script>

<template>
  <div class="page">
    <h1>New Event Type</h1>

    <form @submit.prevent="submit">
      <div class="field">
        <label for="title">Title</label>
        <input
          id="title"
          name="title"
          type="text"
          placeholder="e.g. Consultation"
        >
        <p v-if="errors.title" class="field-error">{{ errors.title }}</p>
      </div>

      <div class="field">
        <label for="description">Description</label>
        <input
          id="description"
          name="description"
          type="text"
          placeholder="Optional description"
        >
      </div>

      <div class="field">
        <label for="duration">Duration (minutes)</label>
        <input
          id="duration"
          name="duration"
          type="number"
          min="5"
        >
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
          <input
            id="bufferBefore"
            name="bufferBefore"
            type="number"
            min="0"
          >
          <p v-if="errors.bufferBefore" class="field-error">{{ errors.bufferBefore }}</p>
        </div>
        <div class="field">
          <label for="bufferAfter">Buffer After (min)</label>
          <input
            id="bufferAfter"
            name="bufferAfter"
            type="number"
            min="0"
          >
          <p v-if="errors.bufferAfter" class="field-error">{{ errors.bufferAfter }}</p>
        </div>
      </div>

      <p v-if="generalError" class="error">{{ generalError }}</p>

      <div class="actions">
        <NuxtLink to="/event-types" class="btn-cancel">Cancel</NuxtLink>
        <button type="submit" class="btn-save" :disabled="isSubmitting">
          {{ isSubmitting ? "Creating..." : "Create" }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.page { max-width: 560px; margin: 0 auto; padding: 2rem 1rem; }
h1 { margin-bottom: 1.5rem; }
.field { margin-bottom: 1rem; }
.field label { display: block; font-weight: 600; margin-bottom: 0.25rem; font-size: 0.9rem; }
.field input, .field select { width: 100%; padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; font-size: 0.95rem; box-sizing: border-box; }
.field-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.field-error { color: #cc0000; font-size: 0.8rem; margin: 0.25rem 0 0; }
.error { color: #cc0000; margin: 1rem 0; }
.actions { display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem; }
.btn-cancel { color: #555; text-decoration: none; padding: 0.5rem 1rem; border: 1px solid #ccc; border-radius: 6px; font-size: 0.95rem; }
.btn-cancel:hover { background: #f5f5f5; }
.btn-save { background: #0070f3; color: #fff; border: none; padding: 0.5rem 1.5rem; border-radius: 6px; font-size: 0.95rem; cursor: pointer; }
.btn-save:hover:not(:disabled) { background: #0051cc; }
.btn-save:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
