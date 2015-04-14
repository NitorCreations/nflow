(function () {
  'use strict';

  var m = angular.module('nflowExplorer', [
    'nflowExplorer.about',
    'nflowExplorer.config.console',
    'nflowExplorer.config.routes',
    'nflowExplorer.filters',
    'nflowExplorer.frontPage',
    'nflowExplorer.layout',
    'nflowExplorer.search',
    'nflowExplorer.services',
    'nflowExplorer.services.executorPoller',
    'nflowExplorer.workflow',
    'nflowExplorer.workflowDefinition',
    'nflowExplorer.workflowStats',
    'ngAnimate',
    'ngCookies',
    'ngSanitize',
    'ngTouch',
    'ui.bootstrap',
  ]);

  m.run(function (ExecutorPoller) {
    ExecutorPoller.start();
  });

})();
