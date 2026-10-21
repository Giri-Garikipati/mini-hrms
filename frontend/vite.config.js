import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In development (npm run dev), any request to /api is forwarded to the Spring Boot
// backend on port 8080. So the browser only ever talks to one address and we don't need CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
});
