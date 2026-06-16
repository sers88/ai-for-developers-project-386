import { describe, expect, it } from "vitest"
import { browserTimezone, formatSlotInTimezone } from "../utils/timezone"

describe("browserTimezone", () => {
  it("returns a non-empty IANA timezone", () => {
    const tz = browserTimezone()
    expect(typeof tz).toBe("string")
    expect(tz.length).toBeGreaterThan(0)
  })
})

describe("formatSlotInTimezone", () => {
  // 10:00 Europe/Moscow (UTC+3, no DST) == 07:00 UTC
  const moscowTenAm = "2025-01-15T07:00:00.000Z"

  it("renders the slot in Europe/Moscow", () => {
    expect(formatSlotInTimezone(moscowTenAm, "Europe/Moscow")).toBe("10:00 AM")
  })

  it("converts the same instant to Asia/Yekaterinburg (UTC+5)", () => {
    // Moscow UTC+3 -> 10:00; Yekaterinburg UTC+5 -> 12:00
    expect(formatSlotInTimezone(moscowTenAm, "Asia/Yekaterinburg")).toBe("12:00 PM")
  })

  it("converts the same instant to UTC", () => {
    expect(formatSlotInTimezone(moscowTenAm, "UTC")).toBe("07:00 AM")
  })

  it("falls back to local formatting for an invalid timezone", () => {
    const result = formatSlotInTimezone(moscowTenAm, "Invalid/Foo")
    expect(typeof result).toBe("string")
    expect(result.length).toBeGreaterThan(0)
  })
})
