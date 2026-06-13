import { useApiClient } from "~/api/client"
import type { components } from "~/api/generated/schema"

type Schedule = components["schemas"]["ScheduleResponse"]

export const useSchedules = () => {
  const api = useApiClient()

  async function loadSchedules(): Promise<Schedule[]> {
    const { data, error } = await api.GET("/api/schedules")
    if (error || !data) throw new Error(String(error) || "Failed to load schedules")
    return data
  }

  async function updateSchedule(id: string, body: components["schemas"]["UpdateScheduleRequest"]): Promise<Schedule> {
    const { data, error } = await api.PUT("/api/schedules/{id}", {
      params: { path: { id } },
      body,
    })
    if (error || !data) throw new Error(String(error) || "Failed to update schedule")
    return data
  }

  return { loadSchedules, updateSchedule }
}
