// Configuration for your app
// https://v2.quasar.dev/quasar-cli-vite/quasar-config-file

import { createReadStream, statSync } from 'node:fs';
import { join, normalize, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { federation } from '@module-federation/vite';
import { defineConfig } from '#q-app/wrappers';

export default defineConfig((ctx) => {
  return {
    // https://v2.quasar.dev/quasar-cli-vite/prefetch-feature
    // preFetch: true,

    // app boot file (/src/boot)
    // --> boot files are part of "main.js"
    // https://v2.quasar.dev/quasar-cli-vite/boot-files
    boot: [
      'dayjs',
      'pinia',
      'nunjucks',
      'config',
      'axios',
      'oidc',
      'i18n',
      'module-lifecycle',
      'theme',
      'ui-design',
    ],

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#css
    css: ['app.scss'],

    // https://github.com/quasarframework/quasar/tree/dev/extras
    extras: [
      // 'ionicons-v4',
      // 'mdi-v7',
      // 'fontawesome-v6',
      // 'eva-icons',
      // 'themify',
      // 'line-awesome',
      // 'roboto-font-latin-ext', // this or either 'roboto-font', NEVER both!

      'roboto-font', // optional, you are not bound to it
      'material-icons', // optional, you are not bound to it
    ],

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#build
    build: {
      target: {
        browser: ['es2022', 'firefox115', 'chrome115', 'safari14', 'brave115'],
        node: 'node20',
      },

      typescript: {
        strict: true,
        vueShim: true,
        // extendTsConfig (tsConfig) {}
      },

      vueRouterMode: 'history', // available values: 'hash', 'history'
      // vueRouterBase,
      // vueDevtools,
      // vueOptionsAPI: false,

      // rebuildCache: true, // rebuilds Vite/linter/etc cache on startup

      // publicPath: '/',
      // analyze: true,
      // env: {},
      // rawDefine: {}
      // ignorePublicFolder: true,
      // minify: false,
      // polyfillModulePreload: true,
      // distDir

      extendViteConf(viteConf) {
        viteConf.build = {
          ...viteConf.build,
          target: 'es2022',
          modulePreload: false,
          rollupOptions: {
            output: {
              entryFileNames: 'assets/[name].[hash:8].js',
              chunkFileNames: 'assets/[name].[hash:8].js',
              assetFileNames: 'assets/[name].[hash:8][extname]',
            },
          },
        };
        viteConf.plugins?.push(
          // The API stores the avatars in docker/avatars (see AVATAR_LOCATION in docker/dev/env/api.env).
          // Serve that directory under /avatars, as Nginx does in the Docker environments, so the profile
          // panels display the uploaded images in dev mode too. The files all carry the configured
          // avatar.extension, png by default, which is the type served here.
          {
            name: 'linid-serve-avatars',
            apply: 'serve',
            configureServer(server) {
              const avatarsDir = fileURLToPath(
                new URL('../docker/avatars', import.meta.url)
              );

              // Resolves the requested file under avatarsDir, or undefined for a
              // malformed URL or a path leaving the directory.
              const resolveAvatar = (url = '/') => {
                try {
                  const path = decodeURIComponent(url.split('?')[0] ?? '/');
                  const file = join(avatarsDir, normalize(path));

                  return file.startsWith(avatarsDir + sep) ? file : undefined;
                } catch {
                  return undefined;
                }
              };

              // Always answers without calling next(): a missing avatar must get a
              // 404, not the SPA fallback of Vite to index.html.
              server.middlewares.use('/avatars', (request, response) => {
                const file = resolveAvatar(request.url);
                const stats = file
                  ? statSync(file, { throwIfNoEntry: false })
                  : undefined;

                if (!file || !stats?.isFile()) {
                  response.statusCode = 404;
                  response.end();
                  return;
                }

                response.setHeader('Content-Type', 'image/png');
                response.setHeader('Content-Length', stats.size);
                response.setHeader('Cache-Control', 'no-cache');
                response.setHeader('X-Content-Type-Options', 'nosniff');
                response.setHeader(
                  'Content-Security-Policy',
                  "default-src 'none'; sandbox"
                );
                createReadStream(file)
                  .on('error', () => response.destroy())
                  .pipe(response);
              });
            },
          },
          federation({
            name: 'linid-identity-manager-ui',
            remotes: {},
            shared: {
              vue: {
                singleton: true,
                requiredVersion: '3.5.39',
              },
              'vue-router': {
                singleton: true,
                requiredVersion: '5.1.0',
              },
              quasar: {
                singleton: true,
                requiredVersion: '2.21.1',
              },
              '@linagora/linid-im-front-corelib': {
                singleton: true,
                strictVersion: true,
              } as Record<string, unknown>,
              pinia: {
                singleton: true,
                requiredVersion: '3.0.4',
              },
              'vue-i18n': {
                singleton: true,
                requiredVersion: '11.4.6',
              },
            },
          })
        );
      },
      // viteVuePluginOptions: {},

      vitePlugins: [
        [
          '@intlify/unplugin-vue-i18n/vite',
          {
            // if you want to use Vue I18n Legacy API, you need to set `compositionOnly: false`
            // compositionOnly: false,

            // if you want to use named tokens in your Vue I18n messages, such as 'Hello {name}',
            // you need to set `runtimeOnly: false`
            // runtimeOnly: false,

            ssr: ctx.modeName === 'ssr',

            // you need to set i18n resource including paths !
            include: [fileURLToPath(new URL('./src/i18n', import.meta.url))],
          },
        ],

        [
          'vite-plugin-checker',
          {
            vueTsc: true,
            eslint: {
              lintCommand:
                'eslint -c ./eslint.config.js "./src*/**/*.{ts,js,mjs,cjs,vue}"',
              useFlatConfig: true,
            },
          },
          { server: false },
        ],
      ],
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#devserver
    devServer: {
      port: 9000,
      open: true, // opens browser window automatically
      // Serve the shared dev certificate signed by the local CA (see docs/configuration/certificates.md)
      https: {
        key: '../docker/dev/resources/server.key',
        cert: '../docker/dev/resources/server.crt',
      },
      proxy: {
        '/backend': {
          target: 'https://localhost:8443',
          changeOrigin: true,
          secure: false,
          rewrite: (path: string) => path.replace(/^\/backend/, ''),
        },
        '/auth': {
          target: 'https://localhost:8080',
          changeOrigin: true,
          secure: false,
          rewrite: (path: string) => path.replace(/^\/auth/, ''),
        },
        '/static': {
          target: 'https://localhost:8080',
          changeOrigin: true,
          secure: false,
        },
        '/index.psgi': {
          target: 'https://localhost:8080',
          changeOrigin: true,
          secure: false,
        },
      },
    },

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#framework
    framework: {
      config: {},

      // iconSet: 'material-icons', // Quasar icon set
      // lang: 'en-US', // Quasar language pack

      // For special cases outside of where the auto-import strategy can have an impact
      // (like functional components as one of the examples),
      // you can manually specify Quasar components/directives to be available everywhere:
      //
      // components: [],
      // directives: [],

      // Quasar plugins
      plugins: ['Notify'],
    },

    // animations: 'all', // --- includes all animations
    // https://v2.quasar.dev/options/animations
    animations: [],

    // https://v2.quasar.dev/quasar-cli-vite/quasar-config-file#sourcefiles
    // sourceFiles: {
    //   rootComponent: 'src/App.vue',
    //   router: 'src/router/index',
    //   store: 'src/store/index',
    //   pwaRegisterServiceWorker: 'src-pwa/register-service-worker',
    //   pwaServiceWorker: 'src-pwa/custom-service-worker',
    //   pwaManifestFile: 'src-pwa/manifest.json',
    //   electronMain: 'src-electron/electron-main',
    //   electronPreload: 'src-electron/electron-preload'
    //   bexManifestFile: 'src-bex/manifest.json
    // },

    // https://v2.quasar.dev/quasar-cli-vite/developing-ssr/configuring-ssr
    ssr: {
      prodPort: 3000, // The default port that the production server should use
      // (gets superseded if process.env.PORT is specified at runtime)

      middlewares: [
        'render', // keep this as last one
      ],

      // extendPackageJson (json) {},
      // extendSSRWebserverConf (esbuildConf) {},

      // manualStoreSerialization: true,
      // manualStoreSsrContextInjection: true,
      // manualStoreHydration: true,
      // manualPostHydrationTrigger: true,

      pwa: false,
      // pwaOfflineHtmlFilename: 'offline.html', // do NOT use index.html as name!

      // pwaExtendGenerateSWOptions (cfg) {},
      // pwaExtendInjectManifestOptions (cfg) {}
    },

    // https://v2.quasar.dev/quasar-cli-vite/developing-pwa/configuring-pwa
    pwa: {
      workboxMode: 'GenerateSW', // 'GenerateSW' or 'InjectManifest'
      // swFilename: 'sw.js',
      // manifestFilename: 'manifest.json',
      // extendManifestJson (json) {},
      // useCredentialsForManifestTag: true,
      // injectPwaMetaTags: false,
      // extendPWACustomSWConf (esbuildConf) {},
      // extendGenerateSWOptions (cfg) {},
      // extendInjectManifestOptions (cfg) {}
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-cordova-apps/configuring-cordova
    cordova: {
      // noIosLegacyBuildFlag: true, // uncomment only if you know what you are doing
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-capacitor-apps/configuring-capacitor
    capacitor: {
      hideSplashscreen: true,
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-electron-apps/configuring-electron
    electron: {
      // extendElectronMainConf (esbuildConf) {},
      // extendElectronPreloadConf (esbuildConf) {},

      // extendPackageJson (json) {},

      // Electron preload scripts (if any) from /src-electron, WITHOUT file extension
      preloadScripts: ['electron-preload'],

      // specify the debugging port to use for the Electron app when running in development mode
      inspectPort: 5858,

      bundler: 'packager', // 'packager' or 'builder'

      packager: {
        // https://github.com/electron-userland/electron-packager/blob/master/docs/api.md#options
        // OS X / Mac App Store
        // appBundleId: '',
        // appCategoryType: '',
        // osxSign: '',
        // protocol: 'myapp://path',
        // Windows only
        // win32metadata: { ... }
      },

      builder: {
        // https://www.electron.build/configuration/configuration

        appId: 'host',
      },
    },

    // Full list of options: https://v2.quasar.dev/quasar-cli-vite/developing-browser-extensions/configuring-bex
    bex: {
      // extendBexScriptsConf (esbuildConf) {},
      // extendBexManifestJson (json) {},

      /**
       * The list of extra scripts (js/ts) not in your bex manifest that you want to compile and use in your browser
       * extension. Maybe dynamic use them?
       *
       * Each entry in the list should be a relative filename to /src-bex/.
       *
       * @example
       *   {undefined} ('my-script.ts',
       *   'sub-folder/my-other-script.js');
       */
      extraScripts: [],
    },
  };
});
