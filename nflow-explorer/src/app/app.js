(function () {
  'use strict';

  var m = angular.module('nflowExplorer', [
    'nflowExplorer.about',
    'nflowExplorer.config',
    'nflowExplorer.config.adal',
    'nflowExplorer.components',
    'nflowExplorer.frontPage',
    'nflowExplorer.layout',
    'nflowExplorer.search',
    'nflowExplorer.executors',
    'nflowExplorer.services',
    'nflowExplorer.workflow',
    'nflowExplorer.workflowDefinition',
    'nflowExplorer.workflowStats',

    'ngAnimate',
    'ngCookies',
    'ngResource',
    'ngSanitize',
    'ngTouch',
    'ui.bootstrap',
    'chart.js',
    'AdalAngular',
    'toastr'
  ]);

  m.run(function (EndpointService, ExecutorService, $window, config) {
    if (config.htmlTitle) {
      $window.document.title = config.htmlTitle;
    }
    EndpointService.init();
    ExecutorService.start();
  });

  m.config(function (toastrConfig) {
    angular.extend(toastrConfig, {
      preventOpenDuplicates: true
    });
  });

})();
