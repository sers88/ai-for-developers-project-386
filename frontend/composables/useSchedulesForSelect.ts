import { useApiClient } from "~/api/client"
import type { components } from "~/api/generated/schema"

type Schedule = components["schemas"]["ScheduleResponse"]

export const useSchedulesForSelect = () => {
  const api = useApiClient()

  const loading = ref(true)
  const schedules = ref<Schedule[]>([])
  const error = ref<string | null>(null)

  async function load() {
    loading.value = true
    error.value = null
    try {
      const { data, error: err } = await api.GET("/api/schedules")
      if (err || !data) throw new Error(String(err) || "Failed to load schedules")
      schedules.value = data
    } catch (e) {
      error.value = e instanceof Error ? e.message : "Failed to load schedules"
    } finally {
      loading.value = false
    }
  }

  return { loading, schedules, error, load }
}
