<script setup lang="ts">
import { z } from "zod"

definePageMeta({
  layout: "default",
  middleware: ["auth"],
})

const {
  days,
  slots,
  timezone,
  loading,
  saving,
  generalError,
  load,
  addSlot,
  removeSlot,
  save,
} = useScheduleForm()

const schema = z.object({
  timezone: z.string().min(1, "Timezone is required"),
})

const state = reactive({ timezone: "" })

watchEffect(() => {
  state.timezone = timezone.value
})

function onSave() {
  timezone.value = state.timezone
  void save()
}

const timezones = Intl.supportedValuesOf("timeZone")

await load()
</script>

<template>
  <div class="mx-auto max-w-3xl px-6 py-8">
    <header class="mb-6">
      <h1 class="text-2xl font-bold text-highlighted" data-testid="page-heading">Schedule Settings</h1>
      <p class="mt-1 text-sm text-muted">Set your availability for each day of the week.</p>
    </header>

    <div v-if="loading" class="flex flex-col gap-4" data-testid="schedule-loading">
      <UCard v-for="i in 3" :key="i">
        <div class="space-y-3">
          <USkeleton class="h-5 w-1/4" />
          <USkeleton class="h-10 w-full" />
          <USkeleton class="h-10 w-full" />
        </div>
      </UCard>
    </div>

    <UForm v-else :schema="schema" :state="state" class="flex flex-col gap-6" @submit="onSave">
      <UCard>
        <template #header>
          <span class="font-semibold text-highlighted">Timezone</span>
        </template>
        <UFormField label="Your timezone" name="timezone" hint="Slots are interpreted in this timezone">
          <USelectMenu
            v-model="state.timezone"
            :items="timezones"
            searchable
            searchable-placeholder="Search timezone..."
            placeholder="Select timezone"
            icon="i-lucide-globe"
            data-testid="schedule-timezone"
            class="w-full"
          />
        </UFormField>
      </UCard>

      <UCard
        v-for="day in days"
        :key="day.value"
        data-testid="schedule-day-row"
      >
        <template #header>
          <span class="font-semibold text-highlighted">{{ day.label }}</span>
        </template>
        <div class="flex flex-col gap-3">
          <div
            v-for="(slot, index) in slots[day.value]"
            :key="index"
            class="flex items-center gap-2"
          >
            <UInput
              v-model="slot.startTime"
              type="time"
              icon="i-lucide-clock"
              class="w-36"
            />
            <span class="text-sm text-muted">to</span>
            <UInput
              v-model="slot.endTime"
              type="time"
              class="w-36"
            />
            <UButton
              icon="i-lucide-x"
              color="error"
              variant="ghost"
              size="sm"
              :disabled="slots[day.value].length <= 1"
              aria-label="Remove slot"
              data-testid="schedule-remove-slot"
              @click="removeSlot(day.value, index)"
            />
          </div>
          <UButton
            icon="i-lucide-plus"
            label="Add time"
            color="neutral"
            variant="outline"
            size="sm"
            class="w-fit"
            data-testid="schedule-add-slot"
            @click="addSlot(day.value)"
          />
        </div>
      </UCard>

      <p
        v-if="generalError"
        class="rounded-md bg-error/10 px-3 py-2 text-sm text-error"
        role="alert"
        data-testid="schedule-error"
      >
        {{ generalError }}
      </p>

      <div class="flex justify-end">
        <UButton
          type="submit"
          :loading="saving"
          :disabled="loading"
          icon="i-lucide-save"
          data-testid="schedule-save"
        >
          {{ saving ? "Saving..." : "Save" }}
        </UButton>
      </div>
    </UForm>
  </div>
</template>
