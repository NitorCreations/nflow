(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflowDefinition.tabs', []);

  m.directive('workflowDefinitionTabs', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '='
      },
      bindToController: true,
      controller: 'WorkflowDefinitionTabsCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow-definition/workflowDefinitionTabs.html'
    };
  });

  m.controller('WorkflowDefinitionTabsCtrl', function($rootScope, $scope, WorkflowDefinitionGraphApi, WorkflowDefinitionStats, WorkflowStatsPoller) {
    var self = this;
    self.hasStatistics = false;
    self.selectNode = WorkflowDefinitionGraphApi.onSelectNode;
    self.startRadiator = startRadiator;

    initialize();

    function initialize() {
      updateStateExecutionGraph(self.definition.type);

      // poller polls stats with fixed period
      WorkflowStatsPoller.start(self.definition.type);
      // poller broadcasts when events change
      $scope.$on('workflowStatsUpdated', function (scope, type) {
        if (type === self.definition.type) { updateStateExecutionGraph(type); }
      });
    }

    function updateStateExecutionGraph(type) {
      var stats = WorkflowStatsPoller.getLatest(type);
      if (!self.definition) {
        console.debug('Definition not loaded yet');
        return;
      }
      if (stats) {
        processStats(self.definition, stats);
        self.hasStatistics = drawStateExecutionGraph('#statisticsGraph', stats.stateStatistics, self.definition, WorkflowDefinitionGraphApi.onSelectNode);
      }
    }

    // TODO move to service
    function processStats(definition, stats) {
      var totals = {
        executing: 0,
        queued: 0,
        sleeping: 0,
        nonScheduled: 0,
        totalActive: 0
      };
      if (!stats.stateStatistics) {
        stats.stateStatistics = {};
      }
      _.each(definition.states, function (state) {
        var name = state.name;

        state.stateStatistics = stats.stateStatistics[name] ? stats.stateStatistics[name] : {};

        state.stateStatistics.totalActive = _.reduce(_.values(state.stateStatistics), function (a, b) {
          return a + b;
        }, 0) - (state.stateStatistics.nonScheduled ? state.stateStatistics.nonScheduled : 0);

        _.each(['executing', 'queued', 'sleeping', 'nonScheduled', 'totalActive'], function (stat) {
          totals[stat] += state.stateStatistics[stat] ? state.stateStatistics[stat] : 0;
        });
      });
      definition.stateStatisticsTotal = totals;
      return stats;
    }

    function startRadiator() {
      $rootScope.$broadcast('startRadiator');
    }
  });

})();
