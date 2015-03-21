(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow.tabs', [
   'nflowExplorer.workflow.tabs.actionHistory',
   'nflowExplorer.workflow.tabs.stateVariables',
   'nflowExplorer.workflow.tabs.manage'
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
