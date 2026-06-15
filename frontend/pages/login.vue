<script setup lang="ts">
definePageMeta({
  layout: "auth",
})

const { schema, state, onSubmit, isSubmitting, generalError } = useLoginForm()
const { redirectToGoogle } = useGoogleAuth()
</script>

<template>
  <div class="flex flex-col gap-6">
    <div class="text-center">
      <h1 class="text-2xl font-bold text-highlighted">Login</h1>
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
          placeholder="Enter password"
          autocomplete="current-password"
          data-testid="password"
        />
      </UFormField>

      <UButton
        type="submit"
        :loading="isSubmitting"
        block
        data-testid="login-submit"
      >
        Login
      </UButton>
    </UForm>

    <USeparator label="or" />

    <UButton
      color="neutral"
      variant="outline"
      block
      icon="i-logos-google"
      data-testid="google-login"
      @click="redirectToGoogle"
    >
      Sign in with Google
    </UButton>

    <p class="text-center text-sm text-muted">
      Don't have an account?
      <NuxtLink to="/register" class="font-medium text-primary">Register</NuxtLink>
    </p>
  </div>
</template>
