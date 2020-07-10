/* eslint-env node */
const path = require('path');
const fs = require('fs');
const process = require('process');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const {CleanWebpackPlugin} = require('clean-webpack-plugin');
const ScriptExtHtmlWebpackPlugin = require('script-ext-html-webpack-plugin');
const ForkTsCheckerWebpackPlugin = require('fork-ts-checker-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin');
const TerserJSPlugin = require('terser-webpack-plugin');
const ProgressPlugin = require('webpack/lib/ProgressPlugin');
const merge = require("webpack-merge");
const FaviconsWebpackPlugin = require('favicons-webpack-plugin');
const {TsconfigPathsPlugin} = require('tsconfig-paths-webpack-plugin');

const resolveExtensions = ['.ts', '.tsx', '.js'];
const commonConfig = {
    entry: './src/index.ts',
    devtool: 'source-map',
    plugins: [
        new CleanWebpackPlugin(),
        new HtmlWebpackPlugin({
            title: 'OurTwobe',
            template: "src/index.ejs",
        }),
        new ScriptExtHtmlWebpackPlugin({
            defaultAttribute: 'defer',
        }),
        new ForkTsCheckerWebpackPlugin(),
        new FaviconsWebpackPlugin({
            logo: "./src/app/logo.png",
            inject: true,
            prefix: "assets/favicons/",
        }),
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
                    {
                        loader: 'eslint-loader',
                    },
                ],
            },
            {
                test: /\.s[ac]ss$/i,
                include: path.resolve(__dirname, 'src'),
                use: [
                    'style-loader',
                    { loader: 'css-loader', options: { importLoaders: 1 } },
                    'postcss-loader',
                    'sass-loader',
                ],
            },
            {
                test: /\.(png|svg|jpg|gif)$/,
                include: path.resolve(__dirname, 'src'),
                use: [
                    'file-loader',
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
    },
    stats: {
        chunks: true,
    },
};

// A list of modules that are larger than 244kb,
// just on their own, and trigger asset warnings.
const modulesThatAreJustTooBig = [
    "@firebase/firestore"
].map(name => `npm.${name.replace("/", "~")}`);

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
                    if (filename.startsWith("assets/favicons")) {
                        return false;
                    }
                    if (modulesThatAreJustTooBig.some(name => filename.startsWith(name))) {
                        return false;
                    }
                    return !(/\.(map|LICENSE)$/.test(filename));
                },
            },
            optimization: {
                namedChunks: true,
                moduleIds: 'hashed',
                runtimeChunk: "single",
                minimizer: [
                    new TerserJSPlugin(),
                    new OptimizeCSSAssetsPlugin(),
                ],
                splitChunks: {
                    chunks: 'all',
                    maxAsyncRequests: 50,
                    maxInitialRequests: 50,
                    maxSize: 100_000,
                    cacheGroups: {
                        application: {
                            test: /[\\/]src[\\/]/,
                            name(module) {
                                if (module.type === "css/mini-extract") {
                                    module = module.issuer;
                                }
                                const request = module.userRequest || module.request;
                                if (request === undefined) {
                                    console.log("Failed for ", module);
                                    return "application";
                                }
                                return path.relative(__dirname, request)
                                    .replace("/", "~");
                            }
                        },
                        vendor: {
                            test: /[\\/]node_modules[\\/]/,
                            name(module) {
                                const file = module.context;
                                let currentDir = file;
                                // eslint-disable-next-line no-constant-condition
                                while (true) {
                                    while (!fs.existsSync(path.resolve(currentDir, 'package.json'))) {
                                        if (currentDir.indexOf('node_modules') < 0) {
                                            // we went too far
                                            throw new Error(`Failed to find package.json for ${file}`);
                                        }
                                        currentDir = path.dirname(currentDir);
                                    }
                                    const pkgJson = require(path.resolve(currentDir, 'package.json'));
                                    let packageName = pkgJson['name'];
                                    if (!packageName) {
                                        const requested = pkgJson['_requested'];
                                        packageName = requested && requested['name'];
                                    }
                                    if (!packageName) {
                                        currentDir = path.dirname(currentDir);
                                        continue;
                                    }

                                    return `npm.${packageName.replace("/", "~")}`;
                                }
                            },
                        },
                    },
                },
            },
        });
    }
    return merge.merge(commonConfig, {
        mode: 'development',
        devServer: {
            port: 13444,
            contentBase: './dist',
            hot: true,
            historyApiFallback: true,
            proxy: {
                '/api': {
                    target: "http://localhost:13445",
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
            ]
        },
        resolve: {
            alias: {
                'react-dom': '@hot-loader/react-dom',
            },
        },
        optimization: {
            namedChunks: true,
        },
    });
};
