import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// React 빌드와 로컬 개발 시 dashboard-service 프록시를 설정한다.
export default defineConfig({
  plugins: [react()],
  build: {
    sourcemap: false,
  },
})
