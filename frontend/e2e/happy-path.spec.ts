import { expect, test, type Page } from "@playwright/test"

function uniqueEmail(): string {
  return `e2e-${Date.now()}-${Math.floor(Math.random() * 10000)}@test.com`
}

function nextWeekday(): Date {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  while (date.getDay() === 0 || date.getDay() === 6) {
    date.setDate(date.getDate() + 1)
  }
  return date
}

async function navigateToBookingDay(page: Page, target: Date): Promise<void> {
  const targetMonth = target.toLocaleDateString("en-US", { month: "long", year: "numeric" })

  for (let i = 0; i < 3; i++) {
    const label = await page.locator(".month-label").textContent()
    if (label === targetMonth) break
    await page.locator(".nav-btn").last().click()
  }

  const dayCell = page.locator(".calendar-day:not(.empty)").filter({
    hasText: String(target.getDate()),
  })
  await dayCell.click()
}

test.describe("Happy path: register → schedule → event type → book → cancel", () => {
  test("full user journey", async ({ page }) => {
    test.setTimeout(120_000)

    page.on("dialog", (dialog) => dialog.accept())
    page.on("console", (msg) => console.log(`BROWSER [${msg.type()}]:`, msg.text()))
    page.on("pageerror", (err) => console.log("BROWSER ERROR:", err.message))

    const email = uniqueEmail()

    // ── 1. Register ──────────────────────────────────────────────
    await page.goto("/register")
    await page.locator("#email").fill(email)
    await page.locator("#password").fill("password123")
    await page.locator('button[type="submit"]').click()

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 30_000 })
    await expect(page.locator("h1")).toHaveText("Dashboard")

    // ── 2. Save schedule (default Mon–Fri 09:00–18:00) ──────────
    await page.goto("/schedules")
    await expect(page.locator("h1")).toHaveText("Schedule Settings")
    await expect(page.locator(".day-row").first()).toBeVisible({ timeout: 10_000 })

    await page.locator(".btn-save").click()
    await expect(page.locator(".btn-save")).toContainText("Save", { timeout: 10_000 })

    // ── 3. Create event type ────────────────────────────────────
    await page.goto("/event-types/create")
    await expect(page.locator("h1")).toHaveText("New Event Type")

    await page.locator("#title").fill("E2E Consultation")
    await page.locator("#duration").fill("30")

    const scheduleOption = page.locator("#scheduleId option").nth(1)
    const hasSchedule = await scheduleOption.count()
    if (hasSchedule > 0) {
      const scheduleValue = await scheduleOption.getAttribute("value")
      if (scheduleValue) {
        await page.locator("#scheduleId").selectOption(scheduleValue)
      }
    }

    await page.locator('button[type="submit"]').click()
    await expect(page).toHaveURL(/\/event-types$/, { timeout: 15_000 })

    // ── 4. Get booking URL from API ─────────────────────────────
    const token = await page.evaluate(() => localStorage.getItem("accessToken"))
    expect(token).toBeTruthy()

    const apiBase = await page.evaluate(() => {
      const nuxt = (window as unknown as { __NUXT__: { public: { apiBase: string } } }).__NUXT__
      return nuxt?.public?.apiBase || "http://localhost:8080"
    })

    const eventTypesResp = await page.request.get(`${apiBase}/api/event-types`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(eventTypesResp.ok()).toBeTruthy()
    const eventTypes = await eventTypesResp.json()
    expect(eventTypes.length).toBeGreaterThan(0)

    const bookingUrl = eventTypes[0].bookingUrl as string
    expect(bookingUrl).toBeTruthy()

    // ── 5. Book a slot on the public booking page ───────────────
    await page.goto(bookingUrl)
    await expect(page.locator("h1")).toHaveText("E2E Consultation")

    const targetDate = nextWeekday()
    await navigateToBookingDay(page, targetDate)

    await page.waitForSelector(".slot-btn", { timeout: 15_000 })
    await page.locator(".slot-btn").first().click()

    await page.locator("#guestName").fill("Test Guest")
    await page.locator("#guestEmail").fill("guest@test.com")
    await page.locator(".btn-book").click()

    await expect(page).toHaveURL(/\/success/, { timeout: 15_000 })
    await expect(page.locator("body")).toContainText("Booking Confirmed")

    // ── 6. Cancel booking from dashboard ────────────────────────
    await page.goto("/dashboard")
    await expect(page.locator("h1")).toHaveText("Dashboard")

    await page.waitForSelector(".btn-cancel", { timeout: 10_000 })
    await page.locator(".btn-cancel").first().click()

    await expect(page.locator(".badge.cancelled")).toBeVisible({ timeout: 10_000 })
  })
})
