<script setup lang="ts">
definePageMeta({
  layout: false,
})

 const { submit, errors, isSubmitting, generalError } = useLoginForm()
const { redirectToGoogle } = useGoogleAuth()
</script>

<template>
  <div class="page">
    <h1>Login</h1>
    <form @submit.prevent="submit">
      <div>
        <label for="email">Email</label>
        <input
          id="email"
          name="email"
          type="email"
          placeholder="email@example.com"
        >
        <p v-if="errors.email" class="error">{{ errors.email }}</p>
      </div>
      <div>
        <label for="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          placeholder="Enter password"
        >
        <p v-if="errors.password" class="error">{{ errors.password }}</p>
      </div>
      <p v-if="generalError" class="error">{{ generalError }}</p>
      <button type="submit" :disabled="isSubmitting">
        {{ isSubmitting ? "Loading..." : "Login" }}
      </button>
    </form>
    <div class="oauth-divider">
      <span>or</span>
    </div>
    <button class="google-btn" @click="redirectToGoogle">
      Sign in with Google
    </button>
    <p>
      Don't have an account? <NuxtLink to="/register">Register</NuxtLink>
    </p>
  </div>
</template>
