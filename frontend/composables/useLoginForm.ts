import { z } from "zod"

const schema = z.object({
  email: z.string().min(1, "Email is required").email("Invalid email format"),
  password: z.string().min(1, "Password is required"),
})

export function useLoginForm() {
  const generalError = ref<string | null>(null)
  const isSubmitting = ref(false)
  const state = reactive({ email: "", password: "" })

  async function onSubmit() {
    generalError.value = null
    isSubmitting.value = true
    try {
      await useAuth().login({ email: state.email, password: state.password })
      await navigateTo("/dashboard")
    } catch {
      generalError.value = "Invalid email or password"
    } finally {
      isSubmitting.value = false
    }
  }

  return { schema, state, onSubmit, isSubmitting, generalError }
}
