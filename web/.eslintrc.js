module.exports = {
    root: true,
    parser: '@typescript-eslint/parser',
    plugins: [
        '@typescript-eslint',
    ],
    extends: [
        'eslint:recommended',
        'plugin:@typescript-eslint/eslint-recommended',
        'plugin:@typescript-eslint/recommended',
        "plugin:react/recommended",
        "plugin:react-hooks/recommended",
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
    "overrides": [
        {
            "files": ["*.js"],
            "rules": {
                "@typescript-eslint/no-var-requires": "off",
                "@typescript-eslint/explicit-function-return-type": "off",
            },
        },
    ],
};
