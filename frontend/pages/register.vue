<script setup lang="ts">
definePageMeta({
  layout: "auth",
})

 const { submit, errors, isSubmitting, generalError, email, password } = useRegisterForm()
const { redirectToGoogle } = useGoogleAuth()
</script>

<template>
  <div class="page">
    <h1>Register</h1>
    <form @submit.prevent="submit">
      <div>
        <label for="email">Email</label>
        <input
          id="email"
          v-model="email"
          name="email"
          type="email"
          placeholder="email@example.com"
          data-testid="email"
        >
        <p v-if="errors.email" class="error">{{ errors.email }}</p>
      </div>
      <div>
        <label for="password">Password</label>
        <input
          id="password"
          v-model="password"
          name="password"
          type="password"
          placeholder="Min. 6 characters"
          data-testid="password"
        >
        <p v-if="errors.password" class="error">{{ errors.password }}</p>
      </div>
      <p v-if="generalError" class="error">{{ generalError }}</p>
        <button type="submit" :disabled="isSubmitting" data-testid="register-submit">
          {{ isSubmitting ? "Loading..." : "Register" }}
        </button>
    </form>
    <div class="oauth-divider">
      <span>or</span>
    </div>
    <button class="google-btn" @click="redirectToGoogle">
      Sign in with Google
    </button>
    <p>
      Already have an account? <NuxtLink to="/login">Login</NuxtLink>
    </p>
  </div>
</template>
