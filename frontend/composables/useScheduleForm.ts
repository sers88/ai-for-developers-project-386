import type { components } from "~/api/generated/schema"

type AvailabilitySlot = components["schemas"]["ScheduleResponse"]["availabilities"][number]

const DAYS = [
  { value: "MON", label: "Monday" },
  { value: "TUE", label: "Tuesday" },
  { value: "WED", label: "Wednesday" },
  { value: "THU", label: "Thursday" },
  { value: "FRI", label: "Friday" },
  { value: "SAT", label: "Saturday" },
  { value: "SUN", label: "Sunday" },
] as const

type DayOfWeek = (typeof DAYS)[number]["value"]
type SlotsByDay = Record<DayOfWeek, { startTime: string; endTime: string }[]>

function minutes(time: string): number {
  const [h, m] = time.split(":").map(Number)
  return (h ?? 0) * 60 + (m ?? 0)
}

function formatTime(value: string): string {
  const parts = value.split(":")
  return `${(parts[0] ?? "0").padStart(2, "0")}:${(parts[1] ?? "0").padStart(2, "0")}`
}

export function useScheduleForm() {
  const generalError = ref<string | null>(null)
  const { loadSchedules, updateSchedule } = useSchedules()

  const scheduleId = ref<string | null>(null)
  const timezone = ref("UTC")
  const loading = ref(true)
  const saving = ref(false)

  const slots = ref<SlotsByDay>({
    MON: [],
    TUE: [],
    WED: [],
    THU: [],
    FRI: [],
    SAT: [],
    SUN: [],
  })

  function initFromAvailabilities(availabilities: AvailabilitySlot[]) {
    const byDay: SlotsByDay = {
      MON: [],
      TUE: [],
      WED: [],
      THU: [],
      FRI: [],
      SAT: [],
      SUN: [],
    }
    for (const a of availabilities) {
      if (a.dayOfWeek in byDay) {
        byDay[a.dayOfWeek as DayOfWeek].push({
          startTime: a.startTime,
          endTime: a.endTime,
        })
      }
    }
    for (const day of DAYS) {
      if (byDay[day.value].length === 0) {
        byDay[day.value].push({ startTime: "09:00", endTime: "18:00" })
      }
    }
    slots.value = byDay
  }

  async function load() {
    loading.value = true
    generalError.value = null
    try {
      const schedules = await loadSchedules()
      if (schedules.length > 0) {
        const first = schedules[0]!
        scheduleId.value = first.id
        timezone.value = first.timezone
        initFromAvailabilities(first.availabilities)
      }
    } catch (e) {
      generalError.value = e instanceof Error ? e.message : "Failed to load"
    } finally {
      loading.value = false
    }
  }

  function addSlot(day: DayOfWeek) {
    slots.value[day].push({ startTime: "09:00", endTime: "18:00" })
  }

  function removeSlot(day: DayOfWeek, index: number) {
    if (slots.value[day].length > 1) {
      slots.value[day].splice(index, 1)
    }
  }

  async function save() {
    if (!scheduleId.value) return
    saving.value = true
    generalError.value = null
    try {
      const availabilities: { dayOfWeek: "MON" | "TUE" | "WED" | "THU" | "FRI" | "SAT" | "SUN"; startTime: string; endTime: string }[] = []
      for (const day of DAYS) {
        const sorted = [...slots.value[day.value]].sort(
          (a, b) => minutes(formatTime(a.startTime)) - minutes(formatTime(b.startTime)),
        )
        for (const slot of sorted) {
          availabilities.push({
            dayOfWeek: day.value as "MON" | "TUE" | "WED" | "THU" | "FRI" | "SAT" | "SUN",
            startTime: formatTime(slot.startTime),
            endTime: formatTime(slot.endTime),
          })
        }
      }
      await updateSchedule(scheduleId.value, { availabilities })
    } catch (e) {
      generalError.value = e instanceof Error ? e.message : "Failed to save"
    } finally {
      saving.value = false
    }
  }

  return { days: DAYS, slots, scheduleId, timezone, loading, saving, generalError, load, addSlot, removeSlot, save }
}
