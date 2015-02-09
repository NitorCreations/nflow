(function () {
  'use strict';

  var m = angular.module('nflowVisApp.frontPage', [
    'nflowVisApp.frontPage.definitionList'
  ]);

  m.controller('FrontPageCtrl', function FrontPageCtrl($scope, $rootScope, WorkflowDefinitions) {
    var vm = this;
    vm.definitions = WorkflowDefinitions.query();

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

})();
