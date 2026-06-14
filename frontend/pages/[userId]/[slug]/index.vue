<script setup lang="ts">
import type { components } from "~/api/generated/schema"

definePageMeta({
  layout: "public",
})

const route = useRoute()
const { getPublicEventType, getAvailability, createBooking } = usePublicBooking()

const userId = route.params.userId as string
const slug = route.params.slug as string

const eventType = ref<PublicEventTypeResponse | null>(null)
const loadingEvent = ref(true)
const loadError = ref<string | null>(null)

const selectedDate = ref<string | null>(null)
const slots = ref<AvailabilitySlot[]>([])
const loadingSlots = ref(false)
const selectedSlot = ref<AvailabilitySlot | null>(null)

const guestName = ref("")
const guestEmail = ref("")
const guestNotes = ref("")
const submitting = ref(false)
const bookingError = ref<string | null>(null)

const today = new Date()
const currentMonth = ref(new Date(today.getFullYear(), today.getMonth(), 1))

type PublicEventTypeResponse = components["schemas"]["PublicEventTypeResponse"]
type AvailabilitySlot = components["schemas"]["AvailabilitySlot"]

try {
  eventType.value = await getPublicEventType(userId, slug)
} catch (e) {
  loadError.value = e instanceof Error ? e.message : "Event type not found"
} finally {
  loadingEvent.value = false
}

const monthDays = computed(() => {
  const year = currentMonth.value.getFullYear()
  const month = currentMonth.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const days: (Date | null)[] = []

  const startOffset = (firstDay.getDay() + 6) % 7
  for (let i = 0; i < startOffset; i++) days.push(null)
  for (let d = 1; d <= lastDay.getDate(); d++) {
    days.push(new Date(year, month, d))
  }
  return days
})

const weekDays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]

const monthLabel = computed(() =>
  currentMonth.value.toLocaleDateString("en-US", { month: "long", year: "numeric" }),
)

function prevMonth() {
  currentMonth.value = new Date(
    currentMonth.value.getFullYear(),
    currentMonth.value.getMonth() - 1,
    1,
  )
}

function nextMonth() {
  currentMonth.value = new Date(
    currentMonth.value.getFullYear(),
    currentMonth.value.getMonth() + 1,
    1,
  )
}

function isPast(date: Date): boolean {
  const todayMidnight = new Date(today.getFullYear(), today.getMonth(), today.getDate())
  return date < todayMidnight
}

function formatDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, "0")
  const d = String(date.getDate()).padStart(2, "0")
  return `${y}-${m}-${d}`
}

async function selectDate(date: Date) {
  if (isPast(date)) return
  selectedDate.value = formatDate(date)
  selectedSlot.value = null
  slots.value = []
  loadingSlots.value = true
  try {
    if (eventType.value) {
      slots.value = await getAvailability(eventType.value.id, selectedDate.value)
    }
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
  return new Date(iso).toLocaleTimeString("en-US", {
    hour: "2-digit",
    minute: "2-digit",
  })
}

async function submitBooking() {
  if (!eventType.value || !selectedSlot.value) return
  if (!guestName.value.trim() || !guestEmail.value.trim()) return

  submitting.value = true
  bookingError.value = null

  try {
    const booking = await createBooking({
      eventTypeId: eventType.value.id,
      guestName: guestName.value.trim(),
      guestEmail: guestEmail.value.trim(),
      notes: guestNotes.value.trim() || undefined,
      startTime: selectedSlot.value.start,
      endTime: selectedSlot.value.end,
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
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
  <div class="booking-page">
    <div v-if="loadingEvent" class="loading">Loading...</div>

    <div v-else-if="loadError" class="error-block">
      <h1 data-testid="page-heading">Event Not Found</h1>
      <p>{{ loadError }}</p>
    </div>

    <div v-else-if="eventType" class="booking-container">
      <div class="event-info">
        <h1 data-testid="page-heading">{{ eventType.title }}</h1>
        <p v-if="eventType.description" class="description">{{ eventType.description }}</p>
        <p class="duration">{{ eventType.duration }} min</p>
        <p v-if="eventType.ownerName" class="owner">with {{ eventType.ownerName }}</p>
        <p class="timezone">{{ eventType.timezone }}</p>
      </div>

      <div class="booking-panel">
        <div class="calendar-section">
          <h2>Select a date</h2>
          <div class="calendar-header">
            <button type="button" class="nav-btn" data-testid="calendar-prev-month" @click="prevMonth">&lt;</button>
            <span class="month-label" data-testid="calendar-month-label">{{ monthLabel }}</span>
            <button type="button" class="nav-btn" data-testid="calendar-next-month" @click="nextMonth">&gt;</button>
          </div>
          <div class="calendar-grid">
            <div v-for="day in weekDays" :key="day" class="week-day">{{ day }}</div>
            <div
              v-for="(day, idx) in monthDays"
              :key="idx"
              :data-testid="day ? 'calendar-day' : undefined"
              class="calendar-day"
              :class="{
                empty: !day,
                disabled: day && isPast(day),
                selected: day && selectedDate === formatDate(day),
              }"
              @click="day && !isPast(day) && selectDate(day)"
            >
              <span v-if="day">{{ day.getDate() }}</span>
            </div>
          </div>
        </div>

        <div v-if="selectedDate" class="slots-section">
          <h2>Available times</h2>
          <div v-if="loadingSlots" class="loading">Loading slots...</div>
          <div v-else-if="slots.length === 0" class="no-slots">No available times</div>
          <div v-else class="slot-list">
            <button
              v-for="slot in slots"
              :key="slot.start"
              type="button"
              class="slot-btn"
              :class="{ active: selectedSlot?.start === slot.start }"
              data-testid="time-slot"
              @click="selectSlot(slot)"
            >
              {{ formatSlotTime(slot.start) }}
            </button>
          </div>
        </div>

        <div v-if="selectedSlot" class="form-section">
          <h2>Your details</h2>
          <form @submit.prevent="submitBooking">
            <div class="field">
              <label for="guestName">Name</label>
              <input
                id="guestName"
                v-model="guestName"
                type="text"
                placeholder="Your name"
                data-testid="guest-name"
                required
              >
            </div>
            <div class="field">
              <label for="guestEmail">Email</label>
              <input
                id="guestEmail"
                v-model="guestEmail"
                type="email"
                placeholder="you@example.com"
                data-testid="guest-email"
                required
              >
            </div>
            <div class="field">
              <label for="guestNotes">Notes (optional)</label>
              <textarea
                id="guestNotes"
                v-model="guestNotes"
                placeholder="Anything you'd like to add?"
                rows="3"
              />
            </div>
            <p v-if="bookingError" class="error">{{ bookingError }}</p>
            <button type="submit" class="btn-book" :disabled="submitting" data-testid="confirm-booking">
              {{ submitting ? "Booking..." : "Confirm Booking" }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.booking-page { max-width: 900px; margin: 0 auto; padding: 2rem 1rem; }
.loading { text-align: center; padding: 3rem; color: #666; }
.error-block { text-align: center; padding: 3rem; }
.error-block h1 { margin-bottom: 0.5rem; }
.error-block p { color: #666; }

.booking-container { display: grid; grid-template-columns: 300px 1fr; gap: 2rem; }
.event-info h1 { margin-bottom: 0.5rem; }
.event-info .description { color: #555; margin-bottom: 0.5rem; }
.event-info .duration { font-weight: 600; color: #0070f3; }
.event-info .owner { color: #555; }
.event-info .timezone { color: #999; font-size: 0.85rem; }

.booking-panel { display: flex; flex-direction: column; gap: 1.5rem; }
.calendar-section h2, .slots-section h2, .form-section h2 { font-size: 1.1rem; margin-bottom: 0.75rem; }

.calendar-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.75rem; }
.month-label { font-weight: 600; }
.nav-btn { border: 1px solid #ccc; background: #fff; border-radius: 4px; padding: 0.25rem 0.75rem; cursor: pointer; font-size: 1rem; }
.nav-btn:hover { background: #f5f5f5; }

.calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px; }
.week-day { text-align: center; font-size: 0.75rem; color: #999; padding: 0.25rem; }
.calendar-day { text-align: center; padding: 0.5rem 0; border-radius: 4px; cursor: pointer; font-size: 0.9rem; }
.calendar-day:hover:not(.empty):not(.disabled) { background: #e6f0ff; }
.calendar-day.empty { visibility: hidden; }
.calendar-day.disabled { color: #ccc; cursor: not-allowed; }
.calendar-day.selected { background: #0070f3; color: #fff; }

.no-slots { color: #999; padding: 1rem 0; }
.slot-list { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.slot-btn { border: 1px solid #ccc; background: #fff; border-radius: 6px; padding: 0.5rem 1rem; cursor: pointer; font-size: 0.9rem; }
.slot-btn:hover { border-color: #0070f3; }
.slot-btn.active { background: #0070f3; color: #fff; border-color: #0070f3; }

.field { margin-bottom: 0.75rem; }
.field label { display: block; font-weight: 600; margin-bottom: 0.25rem; font-size: 0.85rem; }
.field input, .field textarea { width: 100%; padding: 0.5rem; border: 1px solid #ccc; border-radius: 4px; font-size: 0.9rem; box-sizing: border-box; }
.field textarea { resize: vertical; }

.error { color: #cc0000; margin: 0.5rem 0; font-size: 0.85rem; }
.btn-book { background: #0070f3; color: #fff; border: none; padding: 0.6rem 1.5rem; border-radius: 6px; font-size: 0.95rem; cursor: pointer; width: 100%; margin-top: 0.5rem; }
.btn-book:hover:not(:disabled) { background: #0051cc; }
.btn-book:disabled { opacity: 0.5; cursor: not-allowed; }

@media (max-width: 700px) {
  .booking-container { grid-template-columns: 1fr; }
}
</style>
