const { join } = require("path");
module.exports = {
    root: true,
    ignorePatterns: ['.eslintrc.js'],
    parser: '@typescript-eslint/parser',
    parserOptions: {
        ecmaVersion: 2019,
        project: join(__dirname, "./tsconfig.json"),
        sourceType: "module"
    },
    plugins: [
        '@typescript-eslint',
    ],
    extends: [
        'eslint:recommended',
        'plugin:@typescript-eslint/eslint-recommended',
        'plugin:@typescript-eslint/recommended',
        "plugin:react/recommended",
        "plugin:react-hooks/recommended",
        'plugin:rxjs/recommended',
    ],
    settings: {
        "react": {
            "version": "detect"
        },
    },
    rules: {
        "semi": ["error", "always"],
        "comma-dangle": ["error", "only-multiline"],
        "react/prop-types": "off",
        "@typescript-eslint/explicit-function-return-type": [
            "warn",
            {
                "allowConciseArrowFunctionExpressionsStartingWithVoid": true,
            },
        ],
    },
    overrides: [
        {
            "files": ["*.js"],
            "rules": {
                "@typescript-eslint/no-var-requires": "off",
                "@typescript-eslint/explicit-function-return-type": "off",
            },
        },
    ],
};
