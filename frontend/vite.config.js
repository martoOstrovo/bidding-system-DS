import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxy = Object.fromEntries(
    [
      '/auth',
      '/oauth2',
      '/login',
      '/logout',
      '/account-service',
      '/bidding-service',
      '/item-service',
      '/uploads',
    ].map((path) => [
      path,
      { target: env.GATEWAY_URL || 'http://localhost:8090', changeOrigin: true },
    ]),
  );
  return {
    plugins: [react()],
    server: { port: 5173, strictPort: true, proxy },
    preview: { proxy },
  };
});
