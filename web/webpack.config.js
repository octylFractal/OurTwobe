/* eslint-env node */
const path = require('path');
const process = require('process');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const TerserJSPlugin = require('terser-webpack-plugin');
const ProgressPlugin = require('webpack/lib/ProgressPlugin');
const merge = require("webpack-merge");
const {TsconfigPathsPlugin} = require('tsconfig-paths-webpack-plugin');
const ESLintPlugin = require('eslint-webpack-plugin');
const ImageMinimizerPlugin = require("image-minimizer-webpack-plugin");

const resolveExtensions = ['.ts', '.tsx', '.js'];
const commonConfig = {
    entry: {
        main: './src/index.ts',
    },
    devtool: 'source-map',
    plugins: [
        new CleanWebpackPlugin(),
        new HtmlWebpackPlugin({
            title: 'OurTwobe',
            template: "src/index.ejs",
            favicon: "src/app/logo.png",

        }),
        new ForkTsCheckerWebpackPlugin(),
        new ESLintPlugin(),
        new ProgressPlugin({}),
    ],
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                include: path.resolve(__dirname, 'src'),
                use: [
                    {
                        loader: 'babel-loader',
                        options: {
                            cacheDirectory: true,
                        },
                    },
                    {
                        loader: 'ts-loader',
                        options: {
                            transpileOnly: true,
                        },
                    },
                ],
            },
            {
                test: /\.s[ac]ss$/i,
                include: path.resolve(__dirname, 'src'),
                use: [
                    'style-loader',
                    {loader: 'css-loader', options: {importLoaders: 1}},
                    'postcss-loader',
                    'sass-loader',
                ],
            },
        ],
    },
    resolve: {
        plugins: [
            new TsconfigPathsPlugin({
                extensions: resolveExtensions,
            }),
        ],
        extensions: resolveExtensions,
    },
    output: {
        path: path.resolve(__dirname, 'dist'),
        publicPath: '/',
    },
    stats: {
        chunks: true,
    },
    optimization: {
        runtimeChunk: "single",
        minimize: true,
        minimizer: [
            new ImageMinimizerPlugin({
                minimizer: [
                    {
                        implementation: ImageMinimizerPlugin.sharpMinify,
                    },
                ],
            }),
        ],
    },
};

module.exports = (env, argv) => {
    if (typeof argv !== 'undefined' && argv['mode'] === 'production') {
        process.env.NODE_ENV = "production";
        return merge.merge(commonConfig, {
            plugins: [
                new MiniCssExtractPlugin({
                    filename: '[name].css',
                    chunkFilename: '[id].[contenthash].css',
                    ignoreOrder: false,
                }),
            ],
            mode: 'production',
            output: {
                filename: '[name].[contenthash].js'
            },
            module: {
                rules: [
                    {
                        test: /\.css$/i,
                        use: [
                            {
                                loader: MiniCssExtractPlugin.loader,
                            },
                            'css-loader',
                        ],
                    },
                ],
            },
            performance: {
                assetFilter: function (filename) {
                    return !(/\.(map|LICENSE)$/.test(filename));
                },
            },
            optimization: {
                minimizer: [
                    new TerserJSPlugin(),
                    new CssMinimizerPlugin(),
                ],
                splitChunks: {
                    chunks: 'all',
                    maxAsyncRequests: 50,
                    maxInitialRequests: 50,
                    maxSize: 100_000,
                },
            },
        });
    }
    return merge.merge(commonConfig, {
        mode: 'development',
        devServer: {
            port: 13444,
            hot: true,
            historyApiFallback: true,
            compress: false,
            proxy: {
                '/api': {
                    target: "http://127.0.0.1:13445",
                    pathRewrite: {'^/api': ''}
                },
            },
        },
        output: {
            publicPath: "/",
            filename: '[name].js',
        },
        module: {
            rules: [
                {
                    test: /\.css$/i,
                    use: [
                        'style-loader',
                        'css-loader',
                    ],
                },
            ],
        },
    });
};
