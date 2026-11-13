import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
// The dev server forwards /api to Spring Boot, so the browser sees one origin (no CORS trouble).
export default defineConfig({ plugins: [react()], test: { environment: 'jsdom', setupFiles: './src/test/setup.js' },
  server: { port: 5173, proxy: { '/api': 'http://localhost:8080' } } });
