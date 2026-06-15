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

const scheduleOptions = computed(() => [
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

    <UForm :schema="schema" :state="state" class="flex flex-col gap-4" @submit="onSubmit">
      <UFormField label="Title" name="title">
        <UInput
          v-model="state.title"
          placeholder="e.g. Consultation"
          data-testid="event-title"
        />
      </UFormField>

      <UFormField label="Description" name="description">
        <UInput
          v-model="state.description"
          placeholder="Optional description"
        />
      </UFormField>

      <UFormField label="Duration (minutes)" name="duration">
        <UInput
          v-model.number="state.duration"
          type="number"
          min="5"
          placeholder="30"
          data-testid="event-duration"
        />
      </UFormField>

      <UFormField label="Schedule" name="scheduleId">
        <USelect
          v-model="state.scheduleId"
          :items="scheduleOptions"
          :disabled="schedulesLoading"
          placeholder="Select a schedule"
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
          />
        </UFormField>
        <UFormField label="Buffer After (min)" name="bufferAfter">
          <UInput
            v-model.number="state.bufferAfter"
            type="number"
            min="0"
            placeholder="0"
          />
        </UFormField>
      </div>

      <UCard v-if="slugPreview" data-testid="booking-preview">
        <div class="flex items-center gap-3">
          <UIcon name="i-lucide-link" class="size-5 shrink-0 text-muted" />
          <div class="min-w-0">
            <p class="text-xs font-medium text-muted">Booking link preview</p>
            <code class="text-sm text-default">/{{ slugPreview }}</code>
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
          data-testid="event-create-submit"
        >
          Create
        </UButton>
      </div>
    </UForm>
  </div>
</template>
