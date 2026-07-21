import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Route Authentication requests to the Connection Node
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      // Route History requests to the Persistence Node
      '/api/v1/history': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
      // Proxy the WebSocket upgrade requests
      '/chat': {
        target: 'ws://localhost:8081',
        ws: true,
      }
    }
  }
})
