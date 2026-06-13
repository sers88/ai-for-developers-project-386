import { describe, expect, it } from "vitest"

function minutes(time: string): number {
  const [h, m] = time.split(":").map(Number)
  return h * 60 + m
}

function formatTime(value: string): string {
  const parts = value.split(":")
  return `${parts[0].padStart(2, "0")}:${parts[1].padStart(2, "0")}`
}

function initEmptySlots() {
  const days = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"] as const
  const slots: Record<string, { startTime: string; endTime: string }[]> = {}
  for (const day of days) {
    slots[day] = []
  }
  return slots
}

function hasOverlap(slots: Record<string, { startTime: string; endTime: string }[]>): boolean {
  for (const day of Object.keys(slots)) {
    const daySlots = [...slots[day]].sort((a, b) => minutes(a.startTime) - minutes(b.startTime))
    for (let i = 0; i < daySlots.length - 1; i++) {
      if (minutes(daySlots[i].endTime) > minutes(daySlots[i + 1].startTime)) {
        return true
      }
    }
  }
  return false
}

function hasInvalidTime(slots: Record<string, { startTime: string; endTime: string }[]>): boolean {
  for (const day of Object.keys(slots)) {
    for (const slot of slots[day]) {
      if (minutes(slot.startTime) >= minutes(slot.endTime)) return true
    }
  }
  return false
}

describe("Schedule form logic", () => {
  it("has no overlap for non-overlapping slots", () => {
    const slots = initEmptySlots()
    slots.MON.push({ startTime: "09:00", endTime: "12:00" })
    slots.MON.push({ startTime: "13:00", endTime: "18:00" })
    expect(hasOverlap(slots)).toBe(false)
  })

  it("detects overlapping slots on same day", () => {
    const slots = initEmptySlots()
    slots.TUE.push({ startTime: "09:00", endTime: "14:00" })
    slots.TUE.push({ startTime: "13:00", endTime: "18:00" })
    expect(hasOverlap(slots)).toBe(true)
  })

  it("allows same time on different days", () => {
    const slots = initEmptySlots()
    slots.MON.push({ startTime: "09:00", endTime: "12:00" })
    slots.TUE.push({ startTime: "09:00", endTime: "12:00" })
    expect(hasOverlap(slots)).toBe(false)
  })

  it("detects start time after end time", () => {
    const slots = initEmptySlots()
    slots.WED.push({ startTime: "18:00", endTime: "09:00" })
    expect(hasInvalidTime(slots)).toBe(true)
  })

  it("detects start time equal to end time", () => {
    const slots = initEmptySlots()
    slots.THU.push({ startTime: "12:00", endTime: "12:00" })
    expect(hasInvalidTime(slots)).toBe(true)
  })

  it("formats time correctly", () => {
    expect(formatTime("9:00")).toBe("09:00")
    expect(formatTime("09:00")).toBe("09:00")
    expect(formatTime("23:59")).toBe("23:59")
  })

  it("minutes helper parses time correctly", () => {
    expect(minutes("09:00")).toBe(540)
    expect(minutes("18:00")).toBe(1080)
    expect(minutes("00:00")).toBe(0)
  })
})
