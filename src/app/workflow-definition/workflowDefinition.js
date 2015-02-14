(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflowDefinition', [
    'nflowVisApp.workflowDefinition.graph'
  ]);

  m.controller('WorkflowDefinitionCtrl', function (
    $scope, $rootScope, definition, WorkflowDefinitionStats, WorkflowStatsPoller, SelectedNodeNotifier) {

    var self = this;
    self.hasStatistics = false;
    self.definition = definition;
    self.startRadiator = startRadiator;

    initialize();

    function initialize() {
      SelectedNodeNotifier.initialize();

      updateStateExecutionGraph(self.definition.type);

      // poller polls stats with fixed period
      WorkflowStatsPoller.start(self.definition.type);
      // poller broadcasts when events change
      $scope.$on('workflowStatsUpdated', function (scope, type) {
        if (type === self.definition.type) { updateStateExecutionGraph(type); }
      });
    }

    function startRadiator() {
      $rootScope.$broadcast('startRadiator');
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

    function updateStateExecutionGraph(type) {
      var stats = WorkflowStatsPoller.getLatest(type);
      if (!self.definition) {
        console.debug('Definition not loaded yet');
        return;
      }
      if (stats) {
        processStats(self.definition, stats);
        self.hasStatistics = drawStateExecutionGraph('#statisticsGraph', stats.stateStatistics, self.definition, SelectedNodeNotifier.onSelectNode);
      }
    }

  });

  m.factory('SelectedNodeNotifier', function () {
    var listeners = [];

    var api = {};
    api.initialize = initialize;
    api.onSelectNode = onSelectNode;
    api.addListener = addListener;

    return api;

    function initialize() {
      listeners = [];
    }

    function onSelectNode(id) {
      _.forEach(listeners, function(o) { o.onSelectNode(id); });
    }

    function addListener(o) {
      if (!_.isFunction(o.onSelectNode)) {
        throw 'addListener: listener has no onSelectNode function';
      }
      listeners.push(o);
    }

  });

})();
