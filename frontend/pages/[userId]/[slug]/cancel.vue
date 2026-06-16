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
  <div class="mx-auto max-w-md px-4 py-12">
    <UCard class="text-center">
      <div v-if="cancelled" class="flex flex-col items-center gap-3 py-4">
        <div class="flex size-14 items-center justify-center rounded-full bg-success/10">
          <UIcon name="i-lucide-check" class="size-8 text-success" />
        </div>
        <h1 data-testid="booking-cancelled" class="text-xl font-semibold text-highlighted">
          Booking Cancelled
        </h1>
        <p class="text-sm text-muted">Your booking has been successfully cancelled.</p>
        <UButton
          to="/"
          icon="i-lucide-arrow-left"
          label="Done"
          data-testid="cancel-done"
          class="w-full justify-center"
        />
      </div>

      <div v-else class="flex flex-col items-center gap-4 py-4">
        <div class="flex size-14 items-center justify-center rounded-full bg-error/10">
          <UIcon name="i-lucide-x" class="size-8 text-error" />
        </div>
        <h1 data-testid="cancel-heading" class="text-xl font-semibold text-highlighted">
          Cancel Booking
        </h1>
        <p class="text-sm text-muted">
          Are you sure you want to cancel this booking? This action cannot be undone.
        </p>

        <p
          v-if="error"
          class="w-full rounded-md bg-error/10 px-3 py-2 text-sm text-error"
          role="alert"
          data-testid="cancel-error"
        >
          {{ error }}
        </p>

        <UButton
          color="error"
          :loading="cancelling"
          :disabled="!id || !token"
          label="Yes, Cancel Booking"
          icon="i-lucide-x"
          data-testid="confirm-cancel"
          class="w-full justify-center"
          @click="handleCancel"
        />
        <UButton
          to="/"
          color="neutral"
          variant="ghost"
          label="Keep Booking"
          data-testid="keep-booking"
        />
      </div>
    </UCard>
  </div>
</template>
