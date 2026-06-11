import type { NuxtConfig } from "nuxt/schema";

export default defineNuxtConfig({
  compatibilityDate: "2025-01-01",
  devtools: { enabled: true },
  css: ["~/assets/css/main.css"],
  ssr: false,
} satisfies NuxtConfig);
