(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflow.tabs.stateVariables', []);

  m.directive('workflowTabStateVariables', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        workflow: '='
      },
      bindToController: true,
      controller: 'WorkflowTabStateVariablesCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/tabs/stateVariables.html'
    };
  });

  m.controller('WorkflowTabStateVariablesCtrl', function() {});
})();
