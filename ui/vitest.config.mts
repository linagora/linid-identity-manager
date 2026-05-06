import { quasar, transformAssetUrls } from '@quasar/vite-plugin';
import vue from '@vitejs/plugin-vue';
import { fileURLToPath } from 'node:url';
import tsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vitest/config';

// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    alias: {
      '#q-app/wrappers': fileURLToPath(
        new URL('./tests/mocks/quasar-wrappers.ts', import.meta.url)
      ),
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    passWithNoTests: true,
    setupFiles: ['./tests/setup.ts'],
    include: [
      // Matches vitest tests in 'tests/unit' subfolders
      'tests/unit/**/*.{test,spec}.js',
    ],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      include: ['src/**/*.{ts,js,vue}'],
      exclude: [
        '**/*.spec.ts',
        '**/tests/**',
        '**/test/**',
        'src/env.d.ts',
        'src/types/**',
        'src/i18n/**',
        'src/boot/axios.ts',
        'src/boot/i18n.ts',
        'src/boot/remotes.ts',
        'src/router/**',
        'src/stores/index.ts',
        'src/layouts/MainLayout.vue',
        'src/pages/Homepage.vue',
      ],
    },
  },
  plugins: [
    vue({
      template: { transformAssetUrls },
    }),
    quasar({
      sassVariables: 'src/css/quasar.variables.scss',
    }),
    tsconfigPaths(),
  ],
});
