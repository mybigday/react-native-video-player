import { fixupConfigRules } from '@eslint/compat';
import { FlatCompat } from '@eslint/eslintrc';
import js from '@eslint/js';
import prettier from 'eslint-plugin-prettier';
import { defineConfig } from 'eslint/config';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const compat = new FlatCompat({
  baseDirectory: __dirname,
  recommendedConfig: js.configs.recommended,
  allConfig: js.configs.all,
});

export default defineConfig([
  {
    ignores: [
      'node_modules/',
      'lib/',
      'example/android/',
      'example/ios/',
      'example-tv/android/',
      'example-tv/ios/',
      'example-legacy/android/',
      'example-legacy/ios/',
      '.yarn/',
    ],
  },
  {
    files: ['**/*.{js,jsx,ts,tsx}'],
    extends: fixupConfigRules(compat.extends('@react-native', 'prettier')),
    plugins: { prettier },
    rules: {
      'react/react-in-jsx-scope': 'off',
      'prettier/prettier': 'error',
    },
  },
  {
    // The flat config itself uses `import.meta`, which the React Native preset's
    // Babel parser does not accept. Lint it with the default parser instead.
    files: ['**/*.mjs'],
    extends: [js.configs.recommended, ...compat.extends('prettier')],
    plugins: { prettier },
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
    },
    rules: {
      'prettier/prettier': 'error',
    },
  },
]);
