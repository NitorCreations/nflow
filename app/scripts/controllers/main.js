'use strict';

/**
 * @ngdoc function
 * @name nflowVisApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the nflowVisApp
 */
angular.module('nflowVisApp')
.controller('MainCtrl', function MainCtrl($scope, $rootScope, $interval, config,
                                   WorkflowDefinitions, Executors) {
  $scope.workflows = WorkflowDefinitions.query();

  function updateExecutors() {
    Executors.query(function(executors) {
      console.info('Fetching executors');
      $rootScope.executors = executors;
    });
  }

  updateExecutors();

  if(!$rootScope.executorPollingTask) {
    $rootScope.executorPollingTask = $interval(updateExecutors, config.radiator.pollPeriod * 1000);
  }

})
;
