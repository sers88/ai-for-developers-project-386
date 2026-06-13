import { useForm } from "vee-validate"
import { toTypedSchema } from "@vee-validate/zod"
import { z } from "zod"

const schema = toTypedSchema(
  z.object({
    email: z.string().min(1, "Email is required").email("Invalid email format"),
    password: z.string().min(1, "Password is required"),
  }),
)

export function useLoginForm() {
  const generalError = ref<string | null>(null)
  const { handleSubmit, errors, isSubmitting, defineField } = useForm({
    validationSchema: schema,
  })

  const [email] = defineField("email")
  const [password] = defineField("password")

  const submit = handleSubmit(async (values) => {
    generalError.value = null
    try {
      await useAuth().login(values)
      await navigateTo("/dashboard")
    } catch {
      generalError.value = "Invalid email or password"
    }
  })

  return { submit, errors, isSubmitting, generalError, email, password }
}
