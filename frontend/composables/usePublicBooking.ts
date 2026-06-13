import { useApiClient } from "~/api/client"
import type { components } from "~/api/generated/schema"

type PublicEventType = components["schemas"]["PublicEventTypeResponse"]
type AvailabilitySlot = components["schemas"]["AvailabilitySlot"]
type CreateBookingRequest = components["schemas"]["CreateBookingRequest"]
type BookingResponse = components["schemas"]["BookingResponse"]

export const usePublicBooking = () => {
  const api = useApiClient()

  async function getPublicEventType(userId: string, slug: string): Promise<PublicEventType> {
    const { data, error } = await api.GET("/api/public/{userId}/{slug}", {
      params: { path: { userId, slug } },
    })
    if (error || !data) throw new Error(String(error) || "Failed to load event type")
    return data
  }

  async function getAvailability(
    eventTypeId: string,
    date: string,
  ): Promise<AvailabilitySlot[]> {
    const { data, error } = await api.GET("/api/event-types/{id}/availability", {
      params: { path: { id: eventTypeId }, query: { date } },
    })
    if (error || !data) throw new Error(String(error) || "Failed to load availability")
    return data.slots
  }

  async function createBooking(body: CreateBookingRequest): Promise<BookingResponse> {
    const { data, error } = await api.POST("/api/bookings", { body })
    if (error || !data) throw new Error(String(error) || "Failed to create booking")
    return data
  }

  return { getPublicEventType, getAvailability, createBooking }
}
