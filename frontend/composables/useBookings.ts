import { useApiClient } from "~/api/client"
import type { components } from "~/api/generated/schema"

type Booking = components["schemas"]["BookingResponse"]

export const useBookings = () => {
  const api = useApiClient()

  async function loadBookings(status?: "upcoming" | "past"): Promise<Booking[]> {
    const { data, error } = await api.GET("/api/bookings", {
      params: { query: status ? { status } : undefined },
    })
    if (error || !data) throw new Error(String(error) || "Failed to load bookings")
    return data
  }

  async function cancelBooking(id: string, token?: string): Promise<Booking> {
    const { data, error } = await api.DELETE("/api/bookings/{id}", {
      params: {
        path: { id },
        query: token ? { token } : undefined,
      },
    })
    if (error || !data) throw new Error(String(error) || "Failed to cancel booking")
    return data
  }

  return { loadBookings, cancelBooking }
}
