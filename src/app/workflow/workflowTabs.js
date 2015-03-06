(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflow.tabs', [
   'nflowVisApp.workflow.tabs.actionHistory',
   'nflowVisApp.workflow.tabs.stateVariables',
   'nflowVisApp.workflow.tabs.manage'
  ]);

  m.directive('workflowTabs', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '=',
        workflow: '='
      },
      bindToController: true,
      controller: 'WorkflowTabsCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/workflowTabs.html'
    };
  });

  m.controller('WorkflowTabsCtrl', function() {});
})();
