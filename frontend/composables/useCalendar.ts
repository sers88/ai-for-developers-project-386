import { useApiClient } from "~/api/client"
import type { components } from "~/api/generated/schema"

type CalendarStatus = components["schemas"]["CalendarConnectionStatusResponse"]
type BusySlots = components["schemas"]["CalendarBusySlotsResponse"]
type CreateEventRequest = components["schemas"]["CreateCalendarEventRequest"]
type CalendarEvent = components["schemas"]["CalendarEventResponse"]

export const useCalendar = () => {
  const api = useApiClient()

  async function getConnectionStatus(): Promise<CalendarStatus> {
    const { data, error } = await api.GET("/api/calendar/google/status")
    if (error || !data) {
      throw new Error(String(error) || "Failed to get connection status")
    }
    return data
  }

  async function getBusySlots(
    timeMin: string,
    timeMax: string,
  ): Promise<BusySlots> {
    const { data, error } = await api.GET("/api/calendar/google/events", {
      params: { query: { timeMin, timeMax } },
    })
    if (error || !data) {
      throw new Error(String(error) || "Failed to get busy slots")
    }
    return data
  }

  async function createEvent(body: CreateEventRequest): Promise<CalendarEvent> {
    const { data, error } = await api.POST("/api/calendar/google/events", {
      body,
    })
    if (error || !data) {
      throw new Error(String(error) || "Failed to create event")
    }
    return data
  }

  async function deleteEvent(googleEventId: string): Promise<void> {
    const { error } = await api.DELETE(
      "/api/calendar/google/events/{googleEventId}",
      { params: { path: { googleEventId } } },
    )
    if (error) {
      throw new Error(String(error))
    }
  }

  return { getConnectionStatus, getBusySlots, createEvent, deleteEvent }
}

