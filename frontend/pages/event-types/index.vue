<script setup lang="ts">
import type { components } from "~/api/generated/schema"

definePageMeta({
  layout: "default",
  middleware: ["auth"],
})

type EventType = components["schemas"]["EventTypeResponse"]

const toast = useToast()
const { loadEventTypes, deleteEventType } = useEventTypes()

const loading = ref(true)
const generalError = ref<string | null>(null)
const eventTypes = ref<EventType[]>([])

const deleteTarget = ref<EventType | null>(null)
const deleting = ref(false)
const isDeleteModalOpen = ref(false)

const deleteDescription = computed(() => {
  const target = deleteTarget.value
  if (!target) return ""
  return `"${target.title}" will be permanently deleted. This action cannot be undone.`
})

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

function openDeleteModal(et: EventType) {
  deleteTarget.value = et
  isDeleteModalOpen.value = true
}

async function confirmDelete() {
  const target = deleteTarget.value
  if (!target || deleting.value) return
  deleting.value = true
  try {
    await deleteEventType(target.id)
    await load()
  } catch (e) {
    generalError.value = e instanceof Error ? e.message : "Failed to delete"
  } finally {
    deleting.value = false
    isDeleteModalOpen.value = false
    deleteTarget.value = null
  }
}

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

await load()
</script>

<template>
  <div class="mx-auto max-w-4xl px-6 py-8">
    <header class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-highlighted" data-testid="page-heading">Event Types</h1>
        <p class="mt-1 text-sm text-muted">Manage your bookable event types.</p>
      </div>
      <UButton
        to="/event-types/create"
        icon="i-lucide-plus"
        label="New Event Type"
        color="primary"
        data-testid="event-type-create"
      />
    </header>

    <p
      v-if="generalError"
      class="mb-4 text-sm text-error"
      role="alert"
      data-testid="event-types-error"
    >
      {{ generalError }}
    </p>

    <div v-if="loading" class="flex flex-col gap-4" data-testid="event-types-loading">
      <UCard v-for="i in 3" :key="i">
        <div class="flex items-start justify-between gap-4">
          <div class="flex-1 space-y-2">
            <USkeleton class="h-5 w-2/5" />
            <USkeleton class="h-4 w-3/5" />
            <USkeleton class="h-4 w-1/4" />
          </div>
          <USkeleton class="h-8 w-24" />
        </div>
      </UCard>
    </div>

    <UCard v-else-if="eventTypes.length === 0" data-testid="event-types-empty">
      <div class="flex flex-col items-center gap-3 py-8 text-center">
        <UIcon name="i-lucide-calendar-plus" class="size-10 text-muted" />
        <div>
          <p class="font-medium text-highlighted">No event types yet</p>
          <p class="mt-1 text-sm text-muted">Create your first event type so people can book you.</p>
        </div>
        <UButton
          to="/event-types/create"
          icon="i-lucide-plus"
          label="Create event type"
          color="primary"
          data-testid="event-types-empty-cta"
        />
      </div>
    </UCard>

    <div v-else class="flex flex-col gap-4" data-testid="event-types-list">
      <UCard
        v-for="et in eventTypes"
        :key="et.id"
        :ui="{ body: 'flex items-start justify-between gap-4' }"
      >
        <div class="flex-1">
          <div class="mb-1 flex flex-wrap items-center gap-2">
            <h3 class="text-base font-semibold text-highlighted">{{ et.title }}</h3>
            <UBadge color="primary" variant="subtle" size="sm" data-testid="event-type-duration">
              {{ et.duration }} min
            </UBadge>
            <UBadge color="neutral" variant="subtle" size="sm">
              /{{ et.slug }}
            </UBadge>
          </div>
          <p v-if="et.description" class="mb-1 text-sm text-muted">{{ et.description }}</p>
          <div class="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-muted">
            <span v-if="et.bufferBefore || et.bufferAfter" class="inline-flex items-center gap-1">
              <UIcon name="i-lucide-timer" class="size-4" />
              Buffer: {{ et.bufferBefore }}m / {{ et.bufferAfter }}m
            </span>
            <span v-if="et.scheduleName" class="inline-flex items-center gap-1">
              <UIcon name="i-lucide-clock" class="size-4" />
              {{ et.scheduleName }}
            </span>
          </div>
        </div>
        <div class="flex shrink-0 flex-col gap-2">
          <UButton
            :to="`/event-types/${et.id}`"
            icon="i-lucide-pencil"
            label="Edit"
            color="neutral"
            variant="outline"
            size="sm"
            data-testid="event-type-edit"
          />
          <UButton
            icon="i-lucide-link"
            label="Copy link"
            color="neutral"
            variant="ghost"
            size="sm"
            data-testid="event-type-copy"
            @click="copyBookingUrl(et.bookingUrl)"
          />
          <UButton
            icon="i-lucide-trash-2"
            label="Delete"
            color="error"
            variant="ghost"
            size="sm"
            data-testid="event-type-delete"
            @click="openDeleteModal(et)"
          />
        </div>
      </UCard>
    </div>

    <UModal
      v-model:open="isDeleteModalOpen"
      title="Delete event type?"
      :description="deleteDescription"
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
