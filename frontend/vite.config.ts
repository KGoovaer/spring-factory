/// <reference types="vitest/config" />
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// The production build lands in Spring Boot's static resources, so the jar
// serves the app. Maven runs the build in prepare-package, after the Java tests.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: "../target/classes/static",
    emptyOutDir: true,
  },
  server: {
    proxy: { "/api": "http://localhost:8080" },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test-setup.ts"],
  },
});
