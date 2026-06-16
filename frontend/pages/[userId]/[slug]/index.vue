<script setup lang="ts">
import { z } from "zod"
import { today as getToday, getLocalTimeZone, type CalendarDate, type DateValue } from "@internationalized/date"
import type { DateRange } from "reka-ui"
import type { components } from "~/api/generated/schema"

definePageMeta({
  layout: "public",
})

const route = useRoute()
const { getPublicEventType, getAvailability, createBooking } = usePublicBooking()
const owner = usePublicBookingOwner()

const userId = route.params.userId as string
const slug = route.params.slug as string

type PublicEventTypeResponse = components["schemas"]["PublicEventTypeResponse"]
type AvailabilitySlot = components["schemas"]["AvailabilitySlot"]
type CalendarValue = DateValue | DateRange | DateValue[] | null | undefined

const eventType = ref<PublicEventTypeResponse | null>(null)
const loadingEvent = ref(true)
const loadError = ref<string | null>(null)

const selectedDate = ref<CalendarValue>(undefined)
const slots = ref<AvailabilitySlot[]>([])
const loadingSlots = ref(false)
const selectedSlot = ref<AvailabilitySlot | null>(null)

const submitting = ref(false)
const bookingError = ref<string | null>(null)

const selectedTimezone = ref(browserTimezone())
const timezones = Intl.supportedValuesOf("timeZone")

const minValue = getToday(getLocalTimeZone())

const calendarModel = computed<CalendarDate | undefined>(() => {
  const v = selectedDate.value
  if (!v || Array.isArray(v) || ("start" in v)) return undefined
  return v as CalendarDate
})

const schema = z.object({
  guestName: z.string().min(1, "Name is required"),
  guestEmail: z.string().email("Enter a valid email"),
  guestNotes: z.string().optional(),
})

const state = reactive({
  guestName: "",
  guestEmail: "",
  guestNotes: "",
})

try {
  eventType.value = await getPublicEventType(userId, slug)
  owner.value = eventType.value.ownerName ? { name: eventType.value.ownerName } : null
} catch (e) {
  loadError.value = e instanceof Error ? e.message : "Event type not found"
} finally {
  loadingEvent.value = false
}

function asDateValue(value: CalendarValue): DateValue | undefined {
  if (!value || Array.isArray(value) || ("start" in value && "end" in value)) {
    return undefined
  }
  return value as DateValue
}

async function onSelectDate(value: CalendarValue) {
  selectedDate.value = value
  selectedSlot.value = null
  slots.value = []
  const date = asDateValue(value)
  if (!date || !eventType.value) return
  loadingSlots.value = true
  bookingError.value = null
  try {
    slots.value = await getAvailability(eventType.value.id, date.toString())
  } catch (e) {
    bookingError.value = e instanceof Error ? e.message : "Failed to load slots"
  } finally {
    loadingSlots.value = false
  }
}

function selectSlot(slot: AvailabilitySlot) {
  selectedSlot.value = slot
  bookingError.value = null
}

function formatSlotTime(iso: string): string {
  return formatSlotInTimezone(iso, selectedTimezone.value)
}

async function onSubmit() {
  if (!eventType.value || !selectedSlot.value) return
  submitting.value = true
  bookingError.value = null
  try {
    const booking = await createBooking({
      eventTypeId: eventType.value.id,
      guestName: state.guestName.trim(),
      guestEmail: state.guestEmail.trim(),
      notes: state.guestNotes.trim() || undefined,
      startTime: selectedSlot.value.start,
      endTime: selectedSlot.value.end,
      timezone: selectedTimezone.value,
    })

    await navigateTo({
      path: `/${userId}/${slug}/success`,
      query: { id: booking.id },
    })
  } catch (e) {
    bookingError.value = e instanceof Error ? e.message : "Failed to create booking"
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="mx-auto max-w-5xl px-4 py-8">
    <div v-if="loadingEvent" class="flex justify-center py-16">
      <UIcon name="i-lucide-loader-circle" class="size-8 animate-spin text-muted" />
    </div>

    <UCard v-else-if="loadError" class="mx-auto max-w-md">
      <div class="flex flex-col items-center gap-3 py-6 text-center">
        <UIcon name="i-lucide-calendar-x" class="size-10 text-error" />
        <h1 data-testid="page-heading" class="text-xl font-semibold text-highlighted">
          Event Not Found
        </h1>
        <p class="text-sm text-muted">{{ loadError }}</p>
      </div>
    </UCard>

    <div v-else-if="eventType" class="flex flex-col gap-6">
      <header class="flex flex-col gap-2">
        <h1 data-testid="page-heading" class="text-2xl font-bold text-highlighted">
          {{ eventType.title }}
        </h1>
        <p v-if="eventType.description" class="text-sm text-muted">{{ eventType.description }}</p>
        <div class="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted">
          <span class="inline-flex items-center gap-1">
            <UIcon name="i-lucide-clock" class="size-4" />{{ eventType.duration }} min
          </span>
          <span v-if="eventType.ownerName" class="inline-flex items-center gap-1">
            <UIcon name="i-lucide-user" class="size-4" />{{ eventType.ownerName }}
          </span>
          <span class="inline-flex items-center gap-1">
            <UIcon name="i-lucide-globe" class="size-4" />{{ eventType.timezone }}
          </span>
        </div>
      </header>

      <div class="grid gap-6 md:grid-cols-2">
        <UCard data-testid="booking-calendar-card">
          <template #header>
            <span class="font-semibold text-highlighted">Select a date</span>
          </template>
          <div class="flex justify-center">
            <UCalendar
              :model-value="calendarModel"
              :min-value="minValue"
              data-testid="booking-calendar"
              @update:model-value="onSelectDate"
            />
          </div>
        </UCard>

        <div class="flex flex-col gap-6">
          <UCard v-if="selectedDate">
            <template #header>
              <span class="font-semibold text-highlighted">Available times</span>
            </template>

            <div v-if="loadingSlots" class="flex justify-center py-4">
              <UIcon name="i-lucide-loader-circle" class="size-6 animate-spin text-muted" />
            </div>

            <div
              v-else-if="slots.length === 0"
              class="flex flex-col items-center gap-2 py-6 text-center"
              data-testid="no-slots"
            >
              <UIcon name="i-lucide-calendar-off" class="size-8 text-muted" />
              <p class="text-sm text-muted">No available times on this day</p>
            </div>

            <div v-else class="grid grid-cols-2 gap-2 sm:grid-cols-3">
              <UButton
                v-for="slot in slots"
                :key="slot.start"
                :label="formatSlotTime(slot.start)"
                :color="selectedSlot?.start === slot.start ? 'primary' : 'neutral'"
                :variant="selectedSlot?.start === slot.start ? 'solid' : 'outline'"
                data-testid="time-slot"
                class="justify-center"
                @click="selectSlot(slot)"
              />
            </div>
          </UCard>

          <UCard v-if="selectedSlot">
            <template #header>
              <span class="font-semibold text-highlighted">Your details</span>
            </template>
            <UForm :schema="schema" :state="state" class="flex flex-col gap-4" @submit="onSubmit">
              <UFormField label="Name" name="guestName">
                <UInput
                  v-model="state.guestName"
                  placeholder="Your name"
                  data-testid="guest-name"
                  class="w-full"
                />
              </UFormField>
              <UFormField label="Email" name="guestEmail">
                <UInput
                  v-model="state.guestEmail"
                  type="email"
                  placeholder="you@example.com"
                  data-testid="guest-email"
                  class="w-full"
                />
              </UFormField>
              <UFormField label="Notes (optional)" name="guestNotes">
                <UTextarea
                  v-model="state.guestNotes"
                  :rows="3"
                  placeholder="Anything you'd like to add?"
                  data-testid="guest-notes"
                  class="w-full"
                />
              </UFormField>

              <UFormField label="Your timezone" name="timezone" hint="Slots are shown in this timezone">
                <USelectMenu
                  v-model="selectedTimezone"
                  :items="timezones"
                  searchable
                  searchable-placeholder="Search timezone..."
                  icon="i-lucide-globe"
                  data-testid="booking-timezone"
                  class="w-full"
                />
              </UFormField>

              <p
                v-if="bookingError"
                class="rounded-md bg-error/10 px-3 py-2 text-sm text-error"
                role="alert"
                data-testid="booking-error"
              >
                {{ bookingError }}
              </p>

              <UButton
                type="submit"
                :loading="submitting"
                :label="submitting ? 'Booking...' : 'Confirm Booking'"
                icon="i-lucide-check"
                data-testid="confirm-booking"
                class="w-full justify-center"
              />
            </UForm>
          </UCard>
        </div>
      </div>
    </div>
  </div>
</template>
