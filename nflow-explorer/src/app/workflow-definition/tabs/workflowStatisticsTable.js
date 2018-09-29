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
        definition: '='
      },
      controller: 'WorkflowStatisticsTable',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow-definition/tabs/workflowStatisticsTable.html'
    };
  });

  m.controller('WorkflowStatisticsTable', function(WorkflowDefinitionGraphApi, WorkflowStatsPoller, $scope) {
    $scope.isStateSelected = isStateSelected;
    $scope.selectNode = WorkflowDefinitionGraphApi.onSelectNode;
    $scope.stats = WorkflowStatsPoller.getLatest($scope.definition.type) || {};
    calculateTotals($scope.stats);

    $scope.$on('workflowStatsUpdated', function (scope, type) {
      if (type === $scope.definition.type) {
        $scope.stats = WorkflowStatsPoller.getLatest(type);
        calculateTotals($scope.stats);
      }
    });

    function isStateSelected(state) {
      return state.id === WorkflowDefinitionGraphApi.selectedNode;
    }

    function calculateTotals(stats) {
      stats.stateStatisticsTotal = {
        allInstances: 0,
        created: {
          allInstances: 0,
          queuedInstances: 0
        },
        inProgress: {
          allInstances: 0,
          queuedInstances: 0
        },
        executing: {
          allInstances: 0
        },
        manual: {
          allInstances: 0
        },
        finished: {
          allInstances: 0
        }
      };
      _.forEach(_.keys(stats.stateStatistics), function (stateName) {
        var state = stats.stateStatistics[stateName];
        stats.stateStatisticsTotal.allInstances += state.created.allInstances + state.inProgress.allInstances + state.manual.allInstances +
          state.executing.allInstances + state.finished.allInstances;
        stats.stateStatisticsTotal.created.allInstances += state.created.allInstances;
        stats.stateStatisticsTotal.created.queuedInstances += state.created.queuedInstances;
        stats.stateStatisticsTotal.inProgress.allInstances += state.inProgress.allInstances;
        stats.stateStatisticsTotal.inProgress.queuedInstances += state.inProgress.queuedInstances;
        stats.stateStatisticsTotal.executing.allInstances += state.executing.allInstances;
        stats.stateStatisticsTotal.manual.allInstances += state.manual.allInstances;
        stats.stateStatisticsTotal.finished.allInstances += state.finished.allInstances;
      });
    }

  });
})();
