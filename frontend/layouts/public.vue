<script setup lang="ts">
const owner = usePublicBookingOwner()
</script>

<template>
  <div class="min-h-screen bg-default">
    <header v-if="owner?.name" class="border-b border-default">
      <UContainer class="flex items-center gap-3 py-3">
        <UAvatar :alt="owner.name" size="sm" />
        <div class="min-w-0">
          <p class="text-xs text-muted">Book a session with</p>
          <p class="truncate text-sm font-medium text-highlighted">{{ owner.name }}</p>
        </div>
      </UContainer>
    </header>
    <slot />
  </div>
</template>
