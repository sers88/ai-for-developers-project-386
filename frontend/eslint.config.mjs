import nuxtConfig from "@nuxt/eslint-config";

export default nuxtConfig({
  ignores: ["e2e/**", "playwright.config.ts", "playwright-report/**", "test-results/**"],
});
