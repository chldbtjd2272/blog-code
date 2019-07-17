module.exports = {
  publicPath: '',
  outputDir:  process.env.NODE_ENV === 'production' ? '../main/resources/static' : './dist',
  chainWebpack(config) {
    config.output.filename("js/[name].js");
  },
};