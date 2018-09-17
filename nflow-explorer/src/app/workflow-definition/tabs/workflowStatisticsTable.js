(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflowDefinition.tabs.workflowStatisticsTable', [
    'nflowExplorer.workflowDefinition.graph'
  ]);

  m.directive('workflowStatisticsTable', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '=',
      },
      bindToController: true,
      controller: 'WorkflowStatisticsTable',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow-definition/tabs/workflowStatisticsTable.html'
    };
  });

  m.controller('WorkflowStatisticsTable', function(WorkflowDefinitionGraphApi) {
    var self = this;
    self.isStateSelected = isStateSelected;
    self.selectNode = WorkflowDefinitionGraphApi.onSelectNode;

    function isStateSelected(state) {
      return state.id === WorkflowDefinitionGraphApi.selectedNode;
    }
  });
})();
