(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflowDefinition.tabs.workflowSignalsTable', []);

  m.directive('workflowSignalsTable', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '=',
      },
      bindToController: true,
      controller: 'WorkflowSignalsTable',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow-definition/tabs/workflowSignalsTable.html'
    };
  });

  m.controller('WorkflowSignalsTable', function() {});
})();
