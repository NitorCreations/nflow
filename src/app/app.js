(function () {
  'use strict';

  var m = angular.module('nflowVisApp', [
    'nflowVisApp.about',
    'nflowVisApp.config.console',
    'nflowVisApp.config.routes',
    'nflowVisApp.filters',
    'nflowVisApp.frontPage',
    'nflowVisApp.search',
    'nflowVisApp.services',
    'nflowVisApp.services.executorPoller',
    'nflowVisApp.workflow',
    'nflowVisApp.workflowDefinition',
    'nflowVisApp.workflowStats',
    'ngAnimate',
    'ngCookies',
    'ngSanitize',
    'ngTouch',
    'ui.bootstrap'
  ]);

  m.run(function (ExecutorPoller) {
    ExecutorPoller.start();
  });

  m.controller('NaviCtrl', function ($scope, $location) {
    // nope, $stateParams.radiator wont work here
    $scope.radiator = !!$location.search().radiator;
  });
})();


