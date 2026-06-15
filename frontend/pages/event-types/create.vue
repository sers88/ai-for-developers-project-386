<script setup lang="ts">
definePageMeta({
  layout: "default",
  middleware: ["auth"],
})

const router = useRouter()
const { createEventType } = useEventTypes()
const { load: loadSchedules, schedules, loading: schedulesLoading } = useSchedulesForSelect()
const { schema, state, isSubmitting, generalError } = useEventTypeForm()

await loadSchedules()

const scheduleItems = computed(() => [
  { label: "None", value: "" },
  ...schedules.value.map((s) => ({
    label: `${s.name} (${s.timezone})`,
    value: s.id,
  })),
])

function titleToSlug(title: string): string {
  return title
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
}

const slugPreview = computed(() => titleToSlug(state.title))

const bookingPreview = computed(() => {
  if (!slugPreview.value) return ""
  const config = useRuntimeConfig()
  return `${config.public.apiBase}/${slugPreview.value}`
})

async function onSubmit() {
  generalError.value = null
  try {
    await createEventType({
      title: state.title,
      description: state.description || undefined,
      duration: state.duration,
      scheduleId: state.scheduleId || undefined,
      bufferBefore: state.bufferBefore,
      bufferAfter: state.bufferAfter,
    })
    router.push("/event-types")
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to create event type"
  }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-6 py-8">
    <header class="mb-6">
      <h1 class="text-2xl font-bold text-highlighted" data-testid="page-heading">New Event Type</h1>
      <p class="mt-1 text-sm text-muted">Create a new bookable event type.</p>
    </header>

    <UForm :schema="schema" :state="state" class="flex flex-col gap-5" @submit="onSubmit">
      <UCard>
        <div class="flex flex-col gap-4">
          <UFormField label="Title" name="title" required>
            <UInput
              v-model="state.title"
              placeholder="e.g. Consultation"
              icon="i-lucide-type"
              data-testid="event-title"
            />
          </UFormField>

          <UFormField label="Description" name="description">
            <UInput
              v-model="state.description"
              placeholder="Optional description"
              icon="i-lucide-align-left"
            />
          </UFormField>

          <UFormField label="Duration (minutes)" name="duration" required>
            <UInput
              v-model.number="state.duration"
              type="number"
              min="5"
              placeholder="30"
              icon="i-lucide-clock"
              data-testid="event-duration"
            />
          </UFormField>
        </div>
      </UCard>

      <UCard>
        <div class="flex flex-col gap-4">
          <UFormField label="Schedule" name="scheduleId">
            <USelect
              v-model="state.scheduleId"
              :items="scheduleItems"
              :disabled="schedulesLoading"
              placeholder="Select a schedule"
              icon="i-lucide-calendar"
              data-testid="schedule-select"
              class="w-full"
            />
          </UFormField>

          <div class="grid grid-cols-2 gap-4">
            <UFormField label="Buffer Before (min)" name="bufferBefore">
              <UInput
                v-model.number="state.bufferBefore"
                type="number"
                min="0"
                placeholder="0"
                icon="i-lucide-arrow-up-from-line"
              />
            </UFormField>
            <UFormField label="Buffer After (min)" name="bufferAfter">
              <UInput
                v-model.number="state.bufferAfter"
                type="number"
                min="0"
                placeholder="0"
                icon="i-lucide-arrow-down-to-line"
              />
            </UFormField>
          </div>
        </div>
      </UCard>

      <UCard v-if="bookingPreview" data-testid="booking-preview">
        <div class="flex items-center gap-3">
          <UIcon name="i-lucide-link" class="size-5 shrink-0 text-muted" />
          <div class="min-w-0">
            <p class="text-xs font-medium text-muted">Booking link preview</p>
            <code class="block truncate text-sm text-default">{{ bookingPreview }}</code>
          </div>
        </div>
      </UCard>

      <p
        v-if="generalError"
        class="rounded-md bg-error/10 px-3 py-2 text-sm text-error"
        role="alert"
        data-testid="general-error"
      >
        {{ generalError }}
      </p>

      <div class="flex justify-end gap-3">
        <UButton
          to="/event-types"
          color="neutral"
          variant="ghost"
        >
          Cancel
        </UButton>
        <UButton
          type="submit"
          :loading="isSubmitting"
          icon="i-lucide-plus"
          data-testid="event-create-submit"
        >
          {{ isSubmitting ? "Creating..." : "Create" }}
        </UButton>
      </div>
    </UForm>
  </div>
</template>
