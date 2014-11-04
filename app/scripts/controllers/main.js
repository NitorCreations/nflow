'use strict';

var app = angular.module('nflowVisApp');
app.factory('Executors', function($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-executor');
});

/**
 * @ngdoc function
 * @name nflowVisApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the nflowVisApp
 */
angular.module('nflowVisApp')
.constant('config', new Config())
.controller('MainCtrl', function ($scope, $rootScope, $interval, WorkflowDefinitions, Executors) {
  $scope.workflows = WorkflowDefinitions.query();

  function updateExecutors() {
    Executors.query(function(executors) {
      console.info("Fetch executors");
      $rootScope.executors = executors;
    });
  }

  updateExecutors();

  if(!$rootScope.executorQueryRunning) {
    $rootScope.executorQueryRunning = true;
    $interval(updateExecutors, 15*1000);
  }

})
;
