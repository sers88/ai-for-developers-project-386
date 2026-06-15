import { z } from "zod"

const schema = z.object({
  title: z.string().min(1, "Title is required"),
  description: z.string(),
  duration: z.coerce.number().min(5, "Duration must be at least 5 minutes"),
  scheduleId: z.string(),
  bufferBefore: z.coerce.number().min(0, "Buffer must be non-negative"),
  bufferAfter: z.coerce.number().min(0, "Buffer must be non-negative"),
})

export type EventTypeFormState = z.infer<typeof schema>

export function useEventTypeForm() {
  const generalError = ref<string | null>(null)
  const isSubmitting = ref(false)

  const state = reactive<EventTypeFormState>({
    title: "",
    description: "",
    duration: 30,
    scheduleId: "",
    bufferBefore: 0,
    bufferAfter: 0,
  })

  function initFromEventType(data: {
    title: string
    description?: string | null
    duration: number
    scheduleId?: string | null
    bufferBefore: number
    bufferAfter: number
  }) {
    state.title = data.title
    state.description = data.description ?? ""
    state.duration = data.duration
    state.scheduleId = data.scheduleId ?? ""
    state.bufferBefore = data.bufferBefore
    state.bufferAfter = data.bufferAfter
  }

  function reset() {
    state.title = ""
    state.description = ""
    state.duration = 30
    state.scheduleId = ""
    state.bufferBefore = 0
    state.bufferAfter = 0
    generalError.value = null
  }

  return {
    schema,
    state,
    isSubmitting,
    generalError,
    initFromEventType,
    reset,
  }
}
