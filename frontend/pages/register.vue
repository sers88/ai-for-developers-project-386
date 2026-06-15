<script setup lang="ts">
definePageMeta({
  layout: "auth",
})

const { schema, state, onSubmit, isSubmitting, generalError } = useRegisterForm()
const { redirectToGoogle } = useGoogleAuth()
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="text-center">
      <h1 class="text-2xl font-bold text-highlighted">Register</h1>
    </div>

    <p
      v-if="generalError"
      class="rounded-md bg-error/10 px-3 py-2 text-sm text-error"
      data-testid="general-error"
    >
      {{ generalError }}
    </p>

    <UForm
      :schema="schema"
      :state="state"
      class="flex flex-col gap-4"
      @submit="onSubmit"
    >
      <UFormField label="Email" name="email">
        <UInput
          v-model="state.email"
          type="email"
          placeholder="email@example.com"
          autocomplete="email"
          data-testid="email"
        />
      </UFormField>

      <UFormField label="Password" name="password">
        <UInput
          v-model="state.password"
          type="password"
          placeholder="Min. 6 characters"
          autocomplete="new-password"
          data-testid="password"
        />
      </UFormField>

      <UButton
        type="submit"
        :loading="isSubmitting"
        block
        data-testid="register-submit"
      >
        Register
      </UButton>
    </UForm>

    <USeparator label="or" />

    <UButton
      color="neutral"
      variant="outline"
      block
      icon="i-logos-google"
      data-testid="google-register"
      @click="redirectToGoogle"
    >
      Sign in with Google
    </UButton>

    <p class="text-center text-sm text-muted">
      Already have an account?
      <NuxtLink to="/login" class="font-medium text-primary">Login</NuxtLink>
    </p>
  </div>
</template>
