import { z } from "zod"

const schema = z.object({
  email: z.string().min(1, "Email is required").email("Invalid email format"),
  password: z.string().min(6, "Password must be at least 6 characters"),
})

export function useRegisterForm() {
  const generalError = ref<string | null>(null)
  const isSubmitting = ref(false)
  const state = reactive({ email: "", password: "" })

  async function onSubmit() {
    generalError.value = null
    isSubmitting.value = true
    try {
      await useAuth().register({ email: state.email, password: state.password })
      await navigateTo("/dashboard")
    } catch (e) {
      const err = e as { data?: { message?: string } }
      generalError.value = err?.data?.message || "Registration failed"
    } finally {
      isSubmitting.value = false
    }
  }

  return { schema, state, onSubmit, isSubmitting, generalError }
}
