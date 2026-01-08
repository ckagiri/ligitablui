import { defineConfig } from 'vite';
import { resolve } from 'path';

export default defineConfig({
  build: {
    outDir: resolve(__dirname, 'src/main/resources/static/dist'),
    emptyOutDir: true,

    rollupOptions: {
      input: {
        main: resolve(__dirname, 'src/main/resources/static/css/main.css'),
      },
      output: {
        entryFileNames: 'js/[name]-[hash].js',
        assetFileNames: (assetInfo) => {
          if (assetInfo.name.endsWith('.css')) {
            return 'css/[name]-[hash][extname]';
          }
          return 'assets/[name]-[hash][extname]';
        }
      }
    },

    minify: 'terser',
  },

  css: {
    postcss: {
      plugins: [
        require('tailwindcss'),
        require('autoprefixer'),
      ]
    }
  }
});
