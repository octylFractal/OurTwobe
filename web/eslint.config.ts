import {defineConfig} from "eslint/config";
import js from "@eslint/js";
import tseslint from "typescript-eslint";
import eslintReact from "@eslint-react/eslint-plugin";
import rxjsX from "eslint-plugin-rxjs-x";
import globals from "globals";

export default defineConfig(
    {
        ignores: ["node_modules/", "dist/", "build/"],
    },
    js.configs.recommended,
    tseslint.configs.recommended,
    {
        files: ["**/*.{ts,tsx}"],
        extends: [
            eslintReact.configs["recommended-typescript"],
            rxjsX.configs.recommended,
        ],
        languageOptions: {
            globals: globals.browser,
            parserOptions: {
                projectService: true,
            },
        },
        rules: {
            "@typescript-eslint/explicit-function-return-type": [
                "warn",
                {
                    "allowConciseArrowFunctionExpressionsStartingWithVoid": true,
                },
            ],
        },
    },
    {
        files: ["**/*.{js,mjs,cjs}"],
        extends: [tseslint.configs.disableTypeChecked],
        languageOptions: {
            globals: globals.node,
        },
        rules: {
            "@typescript-eslint/no-require-imports": "off",
        },
    },
    {
        rules: {
            "semi": ["error", "always"],
            "comma-dangle": ["error", "only-multiline"],
        },
    },
);
