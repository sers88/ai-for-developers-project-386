import { useApiClient } from "~/api/client"
import type { components } from "~/api/generated/schema"

type EventType = components["schemas"]["EventTypeResponse"]
type CreateEventTypeRequest = components["schemas"]["CreateEventTypeRequest"]
type UpdateEventTypeRequest = components["schemas"]["UpdateEventTypeRequest"]

export const useEventTypes = () => {
  const api = useApiClient()

  async function loadEventTypes(): Promise<EventType[]> {
    const { data, error } = await api.GET("/api/event-types")
    if (error || !data) throw new Error(String(error) || "Failed to load event types")
    return data
  }

  async function getEventType(id: string): Promise<EventType> {
    const { data, error } = await api.GET("/api/event-types/{id}", {
      params: { path: { id } },
    })
    if (error || !data) throw new Error(String(error) || "Failed to load event type")
    return data
  }

  async function createEventType(body: CreateEventTypeRequest): Promise<EventType> {
    const { data, error } = await api.POST("/api/event-types", { body })
    if (error || !data) throw new Error(String(error) || "Failed to create event type")
    return data
  }

  async function updateEventType(id: string, body: UpdateEventTypeRequest): Promise<EventType> {
    const { data, error } = await api.PUT("/api/event-types/{id}", {
      params: { path: { id } },
      body,
    })
    if (error || !data) throw new Error(String(error) || "Failed to update event type")
    return data
  }

  async function deleteEventType(id: string): Promise<void> {
    const { error } = await api.DELETE("/api/event-types/{id}", {
      params: { path: { id } },
    })
    if (error) throw new Error(String(error))
  }

  return { loadEventTypes, getEventType, createEventType, updateEventType, deleteEventType }
}
