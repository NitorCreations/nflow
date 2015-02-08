'use strict';

/**
 * @ngdoc function
 * @name nflowVisApp.controller:MainCtrl
 * @description
 * # MainCtrl
 * Controller of the nflowVisApp
 */
angular.module('nflowVisApp.frontpage', [])
.controller('FrontpageCtrl', function FrontpageCtrl($scope, $rootScope, WorkflowDefinitions) {
  $scope.workflows = WorkflowDefinitions.query();

  $scope.executorClass = function(executor) {
    var expires = moment(executor.expires);
    var active = moment(executor.active);
    if(active.add(1, 'days').isBefore(new Date())) {
      return;
    }
    if(expires.isBefore(new Date())) {
      return 'warning';
    }
    return 'success';
  };
});
