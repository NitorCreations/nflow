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
    'AdalAngular'
  ]);

  m.run(function (EndpointService, ExecutorService) {
    EndpointService.init();
    ExecutorService.start();
  });

})();
