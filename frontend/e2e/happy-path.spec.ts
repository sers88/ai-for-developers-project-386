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
  const normalize = (s: string | null): string => (s ?? "").replace(/\s+/g, " ").trim()
  const targetMonth = normalize(
    target.toLocaleDateString("en-US", { month: "long", year: "numeric" }),
  )

  for (let i = 0; i < 3; i++) {
    const label = normalize(await page.locator("[data-slot='heading']").textContent())
    if (label === targetMonth) break
    await page.getByRole("button", { name: "Next month" }).click()
  }

  const dayCell = page
    .locator("[data-slot='cellTrigger']:not([data-outside-view])")
    .filter({ hasText: String(target.getDate()) })
  await dayCell.first().click()
}

test.describe("Happy path: register → schedule → event type → book → cancel", () => {
  test("full user journey", async ({ page }) => {
    test.setTimeout(120_000)

    const email = uniqueEmail()

    // ── 1. Register ──────────────────────────────────────────────
    await page.goto("/register")
    await page.getByTestId("email").fill(email)
    await page.getByTestId("password").fill("password123")
    await page.getByTestId("register-submit").click()

    await expect(page).toHaveURL(/\/dashboard/, { timeout: 30_000 })
    await expect(page.getByTestId("page-heading")).toHaveText("Dashboard")

    // ── 2. Save schedule (default Mon–Fri 09:00–18:00) ──────────
    await page.goto("/schedules")
    await expect(page.getByTestId("page-heading")).toHaveText("Schedule Settings")
    await expect(page.getByTestId("schedule-day-row").first()).toBeVisible({ timeout: 10_000 })

    await Promise.all([
      page.waitForResponse(
        (resp) => resp.url().includes("/api/schedules") && resp.request().method() === "PUT",
        { timeout: 15_000 },
      ),
      page.getByTestId("schedule-save").click(),
    ])

    // ── 3. Create event type ────────────────────────────────────
    await page.goto("/event-types/create")
    await expect(page.getByTestId("page-heading")).toHaveText("New Event Type")

    await page.getByTestId("event-title").fill("E2E Consultation")
    await page.getByTestId("event-duration").fill("30")

    // Wait for schedules to load, then select first one via USelect dropdown
    await page.getByTestId("schedule-select").click()
    const scheduleOption = page.getByRole("option").first()
    await scheduleOption.waitFor({ state: "visible", timeout: 10_000 })
    await scheduleOption.click()

    await page.getByTestId("event-create-submit").click()
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
    await expect(page.getByTestId("page-heading")).toHaveText("E2E Consultation")

    const targetDate = nextWeekday()
    await navigateToBookingDay(page, targetDate)

    await expect(page.getByTestId("time-slot").first()).toBeVisible({ timeout: 15_000 })
    await page.getByTestId("time-slot").first().click()

    await page.getByTestId("guest-name").fill("Test Guest")
    await page.getByTestId("guest-email").fill("guest@test.com")

    const [bookingResp] = await Promise.all([
      page.waitForResponse(
        (resp) => resp.url().includes("/api/bookings") && resp.request().method() === "POST",
        { timeout: 15_000 },
      ),
      page.getByTestId("confirm-booking").click(),
    ])
    expect(bookingResp.ok()).toBeTruthy()

    const bookingData = await bookingResp.json()
    await page.goto(`${bookingUrl}/success?id=${bookingData.id}`)
    await expect(page.getByTestId("booking-confirmed")).toBeVisible()

    // ── 6. Cancel booking from dashboard ────────────────────────
    await page.goto("/dashboard")
    await expect(page.getByTestId("page-heading")).toHaveText("Dashboard")

    await expect(page.getByTestId("cancel-booking").first()).toBeVisible({ timeout: 10_000 })
    await page.getByTestId("cancel-booking").first().click()

    await expect(page.getByTestId("confirm-cancel-booking")).toBeVisible({ timeout: 10_000 })
    await page.getByTestId("confirm-cancel-booking").click()

    await expect(page.getByTestId("booking-status-cancelled").first()).toBeVisible({ timeout: 10_000 })
  })
})
