'use strict';

/**
 * @ngdoc function
 * @name nflowVisApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the nflowVisApp
 */
angular.module('nflowVisApp')
.constant('config', new Config())
.controller('MainCtrl', function ($scope, WorkflowDefinitions) {
  $scope.workflows = WorkflowDefinitions.query();
})
;
