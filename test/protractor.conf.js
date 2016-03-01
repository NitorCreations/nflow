// conf.js
exports.config = {
  baseUrl: 'http://localhost:9001',
  directConnect: true,
  specs: ['it/**/*.spec.js'],

  capabilities: {
    browserName: 'chrome',
    'chromeOptions': {
      'args': ['no-sandbox']
    }
  },

  // allows running individual files: grunt itest --suite frontPage,search
  suites: {
    frontPage: 'it/frontPage.spec.js',
    menu: 'it/menu.spec.js',
    search: 'it/search.spec.js',
    workflow: 'it/workflow.spec.js',
    workflowDefinition: 'it/workflowDefinition.spec.js',
  },

  jasmineNodeOpts: {
    onComplete: null,
    isVerbose: true,
    showColors: false,
    includeStackTrace: true,
    showTiming: true
  },

  onPrepare: function(){
    browser.driver.manage().window().setSize(1280, 1024);
  }
};
