import { useForm } from "vee-validate"
import { toTypedSchema } from "@vee-validate/zod"
import { z } from "zod"

const schema = toTypedSchema(
  z.object({
    title: z.string().min(1, "Title is required"),
    description: z.string(),
    duration: z.number().min(5, "Duration must be at least 5 minutes"),
    scheduleId: z.string(),
    bufferBefore: z.number().min(0, "Buffer must be non-negative"),
    bufferAfter: z.number().min(0, "Buffer must be non-negative"),
  }),
)

export function useEventTypeForm() {
  const generalError = ref<string | null>(null)
  const { handleSubmit, errors, isSubmitting, resetForm, defineField } = useForm({
    validationSchema: schema,
    initialValues: {
      title: "",
      description: "",
      duration: 30,
      scheduleId: "",
      bufferBefore: 0,
      bufferAfter: 0,
    },
  })

  const [title] = defineField("title")
  const [description] = defineField("description")
  const [duration] = defineField("duration")
  const [scheduleId] = defineField("scheduleId")
  const [bufferBefore] = defineField("bufferBefore")
  const [bufferAfter] = defineField("bufferAfter")

  function initFromEventType(data: {
    title: string
    description?: string | null
    duration: number
    scheduleId?: string | null
    bufferBefore: number
    bufferAfter: number
  }) {
    resetForm({
      values: {
        title: data.title,
        description: data.description ?? "",
        duration: data.duration,
        scheduleId: data.scheduleId ?? "",
        bufferBefore: data.bufferBefore,
        bufferAfter: data.bufferAfter,
      },
    })
  }

  return { errors, isSubmitting, generalError, handleSubmit, initFromEventType, title, description, duration, scheduleId, bufferBefore, bufferAfter }
}