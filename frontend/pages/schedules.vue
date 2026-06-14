<script setup lang="ts">
definePageMeta({
  middleware: ["auth"],
})

const { days, slots, loading, saving, generalError, load, addSlot, removeSlot, save } =
  useScheduleForm()

await load()
</script>

<template>
  <div class="page">
    <h1 data-testid="page-heading">Schedule Settings</h1>

    <div v-if="loading" class="loading">Loading...</div>

    <div v-else class="schedule-grid">
      <div v-for="day in days" :key="day.value" class="day-row" data-testid="schedule-day-row">
        <div class="day-label">{{ day.label }}</div>
        <div class="day-slots">
          <div v-for="(slot, index) in slots[day.value]" :key="index" class="slot">
            <input
              v-model="slot.startTime"
              type="time"
              class="time-input"
            >
            <span class="slot-separator">to</span>
            <input
              v-model="slot.endTime"
              type="time"
              class="time-input"
            >
            <button
              class="btn-remove"
              :disabled="slots[day.value].length <= 1"
              @click="removeSlot(day.value, index)"
            >
              &times;
            </button>
          </div>
          <button class="btn-add" @click="addSlot(day.value)">+ Add time</button>
        </div>
      </div>
    </div>

    <p v-if="generalError" class="error">{{ generalError }}</p>

    <div class="actions">
      <button class="btn-save" :disabled="saving || loading" data-testid="schedule-save" @click="save">
        {{ saving ? "Saving..." : "Save" }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.page {
  max-width: 680px;
  margin: 0 auto;
  padding: 2rem 1rem;
}
h1 {
  margin-bottom: 1.5rem;
}
.loading {
  text-align: center;
  color: #666;
  padding: 2rem;
}
.schedule-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.day-row {
  display: grid;
  grid-template-columns: 100px 1fr;
  align-items: start;
  gap: 0.75rem;
  padding: 0.5rem 0;
  border-bottom: 1px solid #eee;
}
.day-label {
  font-weight: 600;
  padding-top: 0.25rem;
}
.day-slots {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.slot {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}
.time-input {
  width: 7rem;
  padding: 0.25rem 0.5rem;
  border: 1px solid #ccc;
  border-radius: 4px;
  font-size: 0.9rem;
}
.slot-separator {
  color: #888;
  font-size: 0.85rem;
}
.btn-remove {
  background: none;
  border: none;
  color: #cc0000;
  font-size: 1.2rem;
  cursor: pointer;
  padding: 0 0.25rem;
  line-height: 1;
}
.btn-remove:disabled {
  color: #ccc;
  cursor: not-allowed;
}
.btn-add {
  background: none;
  border: 1px dashed #aaa;
  color: #555;
  padding: 0.35rem 0.75rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  width: fit-content;
}
.btn-add:hover {
  border-color: #333;
  color: #111;
}
.actions {
  margin-top: 2rem;
  display: flex;
  justify-content: flex-end;
}
.btn-save {
  background: #0070f3;
  color: #fff;
  border: none;
  padding: 0.5rem 1.5rem;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
}
.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-save:not(:disabled):hover {
  background: #0051cc;
}
.error {
  color: #cc0000;
  margin-top: 1rem;
}
</style>
