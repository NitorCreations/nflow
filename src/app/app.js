(function () {
  'use strict';

  var m = angular.module('nflowExplorer', [
    'nflowExplorer.about',
    'nflowExplorer.config.console',
    'nflowExplorer.config.routes',
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

  m.constant('config', new Config());

  m.run(function (ExecutorPoller) {
    ExecutorPoller.start();
  });

})();
