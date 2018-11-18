(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflowDefinition.tabs', [
    'nflowExplorer.workflowDefinition.tabs.workflowStatisticsTable',
    'nflowExplorer.workflowDefinition.tabs.workflowSignalsTable'
  ]);

  /** <workflow-definition-tabs></workflow-definition-tabs> */
  m.directive('workflowDefinitionTabs', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '='
      },
      controller: 'WorkflowDefinitionTabsCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow-definition/workflowDefinitionTabs.html'
    };
  });

  m.controller('WorkflowDefinitionTabsCtrl', function($rootScope, $scope, WorkflowDefinitionGraphApi,
                                                      WorkflowStatsPoller) {

    var self = this;
    self._metaStatuses = ['sleeping', 'queued', 'executing', 'manual'];
    self.selectNode = WorkflowDefinitionGraphApi.onSelectNode;
    self.isStateSelected = isStateSelected;
    self.startRadiator = startRadiator;
    self.definition = $scope.definition;

    $scope.type = 'StackedBar';
    $scope.series = self._metaStatuses;
    $scope.options = {
      scales: {
        xAxes: [{
          stacked: true,
          ticks: {
            min: 0,
            stepSize: 1
          }
        }],
        yAxes: [{
          stacked: true
        }]
      }
    };
    $scope.stats = {};

    initialize();

    function initialize() {
      updateStateExecutionGraph(self.definition.type);

      // poller polls stats with fixed period
      WorkflowStatsPoller.start(self.definition.type);
      // poller broadcasts when events change
      $scope.$on('workflowStatsUpdated', function (scope, type) {
        if (type === self.definition.type) {
          updateStateExecutionGraph(type);
        }
      });
      $scope.$on('$destroy', function() {
        WorkflowStatsPoller.stop(self.definition.type);
      });
    }

    function isStateSelected(state) {
      return state.id === WorkflowDefinitionGraphApi.selectedNode;
    }

    function updateStateExecutionGraph(type) {
      $scope.stats = WorkflowStatsPoller.getLatest(type);
      if (!self.definition) {
        console.debug('Definition not loaded yet');
      } else if ($scope.stats) {
        var definitionStateStatistics = $scope.stats.stateStatistics || {};
        $scope.labels = barChartLabels(definitionStateStatistics);
        $scope.data = barChartData($scope.labels, definitionStateStatistics);
      }
    }

    function barChartLabels(definitionStateStatistics) {
      var stateNamesFromStatistics = [];
      _.forEach(_.keys(definitionStateStatistics), function(stateName) {
        var s = definitionStateStatistics[stateName];
        if (s.created.allInstances || s.created.queuedInstances || s.inProgress.allInstances ||
          s.inProgress.queuedInstances || s.executing.allInstances || s.manual.allInstances) {
          stateNamesFromStatistics.push(stateName);
        }
      });
      var stateNamesFromDefinition = _.map(_.reject(self.definition.states, {type: 'end'}), 'id');
      var labels = _.union(stateNamesFromStatistics, stateNamesFromDefinition);
      labels.sort();
      return labels;
    }

    function barChartData(labels, definitionStateStatistics) {
      var data = zeros([self._metaStatuses.length, labels.length]);
      for (var i=0; i<labels.length; i++) {
        var s = definitionStateStatistics[labels[i]];
        if (s) {
          for (var j=0; j<self._metaStatuses.length; j++) {
            if (self._metaStatuses[j] === 'sleeping') {
              data[j][i] = (s.created.allInstances - s.created.queuedInstances) +
                (s.inProgress.allInstances - s.inProgress.queuedInstances);
            } else if (self._metaStatuses[j] === 'queued') {
              data[j][i] = s.created.queuedInstances + s.inProgress.queuedInstances;
            } else if (self._metaStatuses[j] === 'manual') {
              data[j][i] = s.manual.allInstances;
            } else if (self._metaStatuses[j] === 'executing') {
              data[j][i] = s.executing.allInstances;
            } else {
              console.error('Unknown metastatus ' + self._metaStatuses[j]);
            }
          }
        }
      }
      return data;
    }

    function zeros(dimensions) {
      var array = [];
      for (var i = 0; i < dimensions[0]; ++i) {
        array.push(dimensions.length === 1 ? 0 : zeros(dimensions.slice(1)));
      }
      return array;
    }

    function startRadiator() {
      $rootScope.$broadcast('startRadiator');
    }
  });
})();
