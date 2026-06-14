import { useForm, defineRule } from "vee-validate"
import { describe, expect, it } from "vitest"

defineRule("required", (value: string) => {
  if (!value || !value.length) return "This field is required"
  return true
})

defineRule("email", (value: string) => {
  if (!/^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i.test(value)) return "Invalid email format"
  return true
})

defineRule("min", (value: string, [limit]: number[]) => {
  if (!value || value.length < (limit ?? 0)) return `Must be at least ${limit} characters`
  return true
})

describe("Login form validation", () => {
  it("validates email is required", async () => {
    const { errors, handleSubmit } = useForm({
      validationSchema: { email: "required|email", password: "required" },
    })

    await handleSubmit(() => {})(new Event("submit"))
    expect(errors.value.email).toBe("This field is required")
  })

  it("validates password is required", async () => {
    const { errors, handleSubmit } = useForm<{ email: string; password: string }>({
      validationSchema: { email: "required|email", password: "required" },
      initialValues: { email: "test@example.com" },
    })

    await handleSubmit(() => {})(new Event("submit"))
    expect(errors.value.password).toBe("This field is required")
  })

  it("validates email format", async () => {
    const { errors, handleSubmit } = useForm({
      validationSchema: { email: "required|email", password: "required" },
      initialValues: { email: "invalid" },
    })

    await handleSubmit(() => {})(new Event("submit"))
    expect(errors.value.email).toBe("Invalid email format")
  })
})

describe("Register form validation", () => {
  it("validates password min length", async () => {
    const { errors, handleSubmit } = useForm({
      validationSchema: { email: "required|email", password: "required|min:6" },
      initialValues: { email: "test@example.com", password: "123" },
    })

    await handleSubmit(() => {})(new Event("submit"))
    expect(errors.value.password).toBe("Must be at least 6 characters")
  })

  it("validates password is required", async () => {
    const { errors, handleSubmit } = useForm<{ email: string; password: string }>({
      validationSchema: { email: "required|email", password: "required|min:6" },
      initialValues: { email: "test@example.com" },
    })

    await handleSubmit(() => {})(new Event("submit"))
    expect(errors.value.password).toBe("This field is required")
  })

  it("validates email format", async () => {
    const { errors, handleSubmit } = useForm({
      validationSchema: { email: "required|email", password: "required|min:6" },
      initialValues: { email: "not-an-email" },
    })

    await handleSubmit(() => {})(new Event("submit"))
    expect(errors.value.email).toBe("Invalid email format")
  })
})
