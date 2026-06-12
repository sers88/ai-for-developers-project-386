import { useForm } from "vee-validate"
import { toTypedSchema } from "@vee-validate/zod"
import { z } from "zod"

const schema = toTypedSchema(
  z.object({
    email: z.string().min(1, "Email is required").email("Invalid email format"),
    password: z.string().min(6, "Password must be at least 6 characters"),
  }),
)

export function useRegisterForm() {
  const generalError = ref<string | null>(null)
  const { handleSubmit, errors, isSubmitting } = useForm({
    validationSchema: schema,
  })

  const submit = handleSubmit(async (values) => {
    generalError.value = null
    try {
      await useAuth().register(values)
      await navigateTo("/dashboard")
    } catch (e) {
      const err = e as { data?: { message?: string } }
      generalError.value = err?.data?.message || "Registration failed"
    }
  })

  return { submit, errors, isSubmitting, generalError }
}
