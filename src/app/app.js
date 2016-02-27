(function () {
  'use strict';

  var m = angular.module('nflowExplorer', [
    'nflowExplorer.about',
    'nflowExplorer.config',
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
  ]);

  m.run(function (ExecutorService) {
    ExecutorService.start();
  });

})();
