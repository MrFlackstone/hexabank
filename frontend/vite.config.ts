import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// El frontend habla siempre con un mismo origen bajo /api. En desarrollo (npm run dev)
// Vite hace de proxy hacia los servicios que corren por maven en el host; en produccion
// ese mismo enrutado lo hace Nginx (ver frontend/nginx.conf). Asi el codigo de cliente es
// identico en ambos entornos y no necesitamos CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/accounts': { target: 'http://localhost:8081', changeOrigin: true, rewrite: (p) => p.replace(/^\/api/, '') },
      '/api/transfers': { target: 'http://localhost:8082', changeOrigin: true, rewrite: (p) => p.replace(/^\/api/, '') },
      '/api/projections/transfers': { target: 'http://localhost:8083', changeOrigin: true, rewrite: (p) => p.replace(/^\/api\/projections/, '') },
      '/api/ledger': { target: 'http://localhost:8083', changeOrigin: true, rewrite: (p) => p.replace(/^\/api/, '') },
    },
  },
});
