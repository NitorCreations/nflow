'use strict';

/**
 * @ngdoc function
 * @name nflowVisApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the nflowVisApp
 */
angular.module('nflowVisApp')
.controller('MainCtrl', function MainCtrl($scope, $rootScope,
                                           WorkflowDefinitions, ExecutorPoller) {
  $scope.workflows = WorkflowDefinitions.query();
});

