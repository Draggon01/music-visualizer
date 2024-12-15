import { defineConfig } from 'vite';

export default defineConfig({
    root: '.', // Optional, set root to current directory
    build: {
        outDir: 'dist', // Directory for build output
    },
    esbuild: {
        target: 'esnext', // Ensures modern JavaScript syntax is used
    },
});