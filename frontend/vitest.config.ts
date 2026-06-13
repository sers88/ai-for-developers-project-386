import { defineConfig } from "vitest/config"
import vue from "@vitejs/plugin-vue"

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: "happy-dom",
    globals: true,
    exclude: ["node_modules/**", "e2e/**", "playwright.config.ts"],
  },
  resolve: {
    alias: {
      "@": ".",
    },
  },
})
