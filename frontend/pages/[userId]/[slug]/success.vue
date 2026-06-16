<script setup lang="ts">
import type { components } from "~/api/generated/schema"

definePageMeta({
  layout: "public",
})

const route = useRoute()
const { getPublicEventType } = usePublicBooking()
const owner = usePublicBookingOwner()

const userId = route.params.userId as string
const slug = route.params.slug as string

type PublicEventTypeResponse = components["schemas"]["PublicEventTypeResponse"]

const eventType = ref<PublicEventTypeResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

try {
  eventType.value = await getPublicEventType(userId, slug)
  owner.value = eventType.value.ownerName ? { name: eventType.value.ownerName } : null
} catch (e) {
  error.value = e instanceof Error ? e.message : "Failed to load event type"
} finally {
  loading.value = false
}
</script>

<template>
  <div class="mx-auto max-w-md px-4 py-12">
    <div v-if="loading" class="flex justify-center">
      <UIcon name="i-lucide-loader-circle" class="size-8 animate-spin text-muted" />
    </div>

    <UCard v-else class="text-center">
      <div class="flex flex-col items-center gap-3 py-4">
        <div class="flex size-14 items-center justify-center rounded-full bg-success/10">
          <UIcon name="i-lucide-check" class="size-8 text-success" />
        </div>
        <h1 data-testid="booking-confirmed" class="text-xl font-semibold text-highlighted">
          Booking Confirmed!
        </h1>

        <div v-if="eventType" class="w-full rounded-md bg-muted/40 p-4 text-left">
          <p class="font-semibold text-highlighted">{{ eventType.title }}</p>
          <p v-if="eventType.description" class="mt-1 text-sm text-muted">
            {{ eventType.description }}
          </p>
          <p class="mt-2 text-sm text-muted">{{ eventType.duration }} minutes</p>
          <p v-if="eventType.ownerName" class="text-sm text-muted">
            with {{ eventType.ownerName }}
          </p>
          <p class="text-sm text-muted">Timezone: {{ eventType.timezone }}</p>
        </div>

        <p class="text-sm text-muted">A calendar invitation has been sent to your email.</p>

        <UButton
          to="/"
          icon="i-lucide-arrow-left"
          label="Done"
          data-testid="booking-done"
          class="w-full justify-center"
        />
      </div>
    </UCard>
  </div>
</template>
