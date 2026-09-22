export default defineNuxtConfig({
  srcDir: 'src/',
  ssr: false,
  css: ['~/style.css'],
  components: [
    {
      path: '~/components',
      pathPrefix: false,
    },
    {
      path: '~/views',
      pathPrefix: false,
    },
  ],
  devServer: {
    port: 5173,
  },
  nitro: {
    devProxy: {
      '/api': {
        target: 'http://localhost:8080/api',
        changeOrigin: true,
      },
    },
  },
  app: {
    head: {
      htmlAttrs: { lang: 'ko' },
      title: 'RUNMATE',
    },
  },
  compatibilityDate: '2026-09-22',
})
