import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

const backend = process.env.API_URL ?? 'http://localhost:8080'

// In development the Vite server forwards API (and Swagger UI) calls to the Spring Boot backend,
// so the browser sees a single origin and no CORS setup is needed.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/swagger-ui': backend,
      '/swagger-ui.html': backend,
      '/v3/api-docs': backend,
    },
  },
})
