<script setup lang="ts">
import type { components } from "~/api/generated/schema"

definePageMeta({
  layout: "default",
  middleware: ["auth"],
})

type EventType = components["schemas"]["EventTypeResponse"]

const route = useRoute()
const router = useRouter()
const id = route.params.id as string
const toast = useToast()
const { getEventType, updateEventType, deleteEventType } = useEventTypes()
const { load: loadSchedules, schedules, loading: schedulesLoading } = useSchedulesForSelect()
const { schema, state, isSubmitting, generalError, initFromEventType } = useEventTypeForm()

const loading = ref(true)
const error = ref<string | null>(null)
const editing = ref(false)
const eventType = ref<EventType | null>(null)

const deleteModalOpen = ref(false)
const deleting = ref(false)

const scheduleOptions = computed(() => [
  { label: "None", value: "" },
  ...schedules.value.map((s) => ({
    label: `${s.name} (${s.timezone})`,
    value: s.id,
  })),
])

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
    toast.add({
      title: "Booking link copied!",
      icon: "i-lucide-clipboard-check",
      color: "success",
    })
  }).catch(() => {
    toast.add({
      title: "Failed to copy",
      description: url,
      icon: "i-lucide-clipboard-x",
      color: "error",
    })
  })
}

function openDeleteModal() {
  deleteModalOpen.value = true
}

async function confirmDelete() {
  if (deleting.value) return
  deleting.value = true
  try {
    await deleteEventType(id)
    router.push("/event-types")
  } catch (e) {
    error.value = e instanceof Error ? e.message : "Failed to delete"
    deleting.value = false
    deleteModalOpen.value = false
  }
}

async function onSubmit() {
  generalError.value = null
  try {
    await updateEventType(id, {
      title: state.title,
      description: state.description || undefined,
      duration: state.duration,
      scheduleId: state.scheduleId || undefined,
      bufferBefore: state.bufferBefore,
      bufferAfter: state.bufferAfter,
    })
    editing.value = false
    await load()
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to update event type"
  }
}
</script>

<template>
  <div class="mx-auto max-w-2xl px-6 py-8">
    <div v-if="loading" class="flex flex-col gap-4" data-testid="event-type-loading">
      <UCard>
        <div class="space-y-3">
          <USkeleton class="h-7 w-1/3" />
          <USkeleton class="h-5 w-1/2" />
          <USkeleton class="h-20 w-full" />
        </div>
      </UCard>
    </div>

    <p
      v-else-if="error"
      class="rounded-md bg-error/10 px-3 py-2 text-sm text-error"
      role="alert"
      data-testid="event-type-error"
    >
      {{ error }}
    </p>

    <template v-else-if="eventType">
      <!-- Detail view -->
      <div v-if="!editing">
        <header class="mb-6 flex items-start justify-between gap-4">
          <div class="min-w-0">
            <h1 class="text-2xl font-bold text-highlighted" data-testid="page-heading">
              {{ eventType.title }}
            </h1>
            <p v-if="eventType.description" class="mt-1 text-sm text-muted">
              {{ eventType.description }}
            </p>
          </div>
          <div class="flex shrink-0 gap-2">
            <UButton
              icon="i-lucide-pencil"
              label="Edit"
              color="neutral"
              variant="outline"
              data-testid="event-type-edit-btn"
              @click="editing = true"
            />
            <UButton
              icon="i-lucide-trash-2"
              label="Delete"
              color="error"
              variant="ghost"
              data-testid="event-type-delete-btn"
              @click="openDeleteModal"
            />
          </div>
        </header>

        <div class="flex flex-col gap-4">
          <UCard>
            <div class="grid grid-cols-2 gap-4">
              <div>
                <p class="text-xs font-medium text-muted">Duration</p>
                <p class="mt-1 text-sm text-default">{{ eventType.duration }} min</p>
              </div>
              <div>
                <p class="text-xs font-medium text-muted">Slug</p>
                <p class="mt-1 text-sm text-default">
                  <code class="font-mono">/{{ eventType.slug }}</code>
                </p>
              </div>
              <div>
                <p class="text-xs font-medium text-muted">Buffer Before</p>
                <p class="mt-1 text-sm text-default">{{ eventType.bufferBefore }} min</p>
              </div>
              <div>
                <p class="text-xs font-medium text-muted">Buffer After</p>
                <p class="mt-1 text-sm text-default">{{ eventType.bufferAfter }} min</p>
              </div>
              <div v-if="eventType.scheduleName">
                <p class="text-xs font-medium text-muted">Schedule</p>
                <p class="mt-1 text-sm text-default">{{ eventType.scheduleName }}</p>
              </div>
              <div>
                <p class="text-xs font-medium text-muted">Created</p>
                <p class="mt-1 text-sm text-default">
                  {{ new Date(eventType.createdAt).toLocaleDateString() }}
                </p>
              </div>
            </div>
          </UCard>

          <UCard data-testid="booking-link-card">
            <div class="flex items-center justify-between gap-3">
              <div class="flex min-w-0 items-center gap-3">
                <UIcon name="i-lucide-link" class="size-5 shrink-0 text-muted" />
                <div class="min-w-0">
                  <p class="text-xs font-medium text-muted">Booking link</p>
                  <code class="block truncate text-sm text-default">{{ eventType.bookingUrl }}</code>
                </div>
              </div>
              <UButton
                icon="i-lucide-copy"
                label="Copy"
                color="primary"
                variant="soft"
                size="sm"
                data-testid="event-type-copy-btn"
                @click="copyBookingUrl(eventType.bookingUrl)"
              />
            </div>
          </UCard>

          <UButton
            to="/event-types"
            icon="i-lucide-arrow-left"
            label="Back to list"
            color="neutral"
            variant="ghost"
            class="w-fit"
          />
        </div>
      </div>

      <!-- Edit form -->
      <div v-else>
        <header class="mb-6">
          <h1 class="text-2xl font-bold text-highlighted" data-testid="page-heading">
            Edit {{ eventType.title }}
          </h1>
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
              color="neutral"
              variant="ghost"
              data-testid="event-edit-cancel"
              @click="editing = false"
            >
              Cancel
            </UButton>
            <UButton
              type="submit"
              :loading="isSubmitting"
              data-testid="event-edit-submit"
            >
              Save
            </UButton>
          </div>
        </UForm>
      </div>
    </template>

    <UModal
      v-model:open="deleteModalOpen"
      title="Delete event type?"
      :description="`${eventType?.title} will be permanently deleted. This action cannot be undone.`"
      data-testid="delete-event-type-modal"
    >
      <template #footer="{ close }">
        <div class="flex w-full justify-end gap-2">
          <UButton
            label="Keep"
            color="neutral"
            variant="ghost"
            data-testid="delete-event-type-cancel"
            @click="close"
          />
          <UButton
            label="Delete"
            color="error"
            :loading="deleting"
            data-testid="delete-event-type-confirm"
            @click="confirmDelete"
          />
        </div>
      </template>
    </UModal>
  </div>
</template>
