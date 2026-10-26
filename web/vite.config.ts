import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// 管理控制台前端构建配置。
// Admin console frontend build configuration.
//
// 产物输出到 web/dist，由 Maven 的 maven-resources-plugin 拷入 target/classes/static，
// 因此 base 固定为根路径，且资源使用相对路径以适配任意挂载点。
// The output goes to web/dist and the maven-resources-plugin copies it into
// target/classes/static, so the base is the root path and the assets use relative paths to fit any mount point.
export default defineConfig({
  base: './',
  plugins: [react(), tailwindcss()],
  build: {
    outDir: 'dist',
    emptyOutDir: true,
    assetsDir: 'assets',
  },
  server: {
    port: 5173,
    proxy: {
      // 开发期把 API 代理到本地游戏服的管理控制台端口。
      // Proxies the API to the local game server's admin console port during development.
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
      },
    },
  },
})
