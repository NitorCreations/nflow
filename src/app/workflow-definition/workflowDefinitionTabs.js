(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflowDefinition.tabs', [
    'nflowExplorer.barchart'
  ]);

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
    self.isStateSelected = isStateSelected;
    self.startRadiator = startRadiator;
    self.options = {chart: {
                type: 'multiBarChart',
                height: 450,
                preserveAspectRatio: 'xMinYMin',
                viewBox: '0 0 100 100',
                width: 400,
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 160,
                    left: 45
                },
                x: function(d){return d.label;},
                y: function(d){return d.value;},
                clipEdge: true,
                staggerLabels: false,
                reduceXTicks: false,
                showControls: false,
                // not supported by angular-nvd3?
                duration: 0,
                transitionDuration: 0,
                stacked: true,
                xAxis: {
                    axisLabel: 'States',
                    showMaxMin: false,
                    rotateLabels: 25,
                },
                yAxis: {
                    axisLabel: 'Workflow instances',
                    axisLabelDistance: 40,
                    tickFormat: function(d){
                        return d3.format(',.0f')(d);
                    }
                }
            }
    };

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

    function isStateSelected(state) {
      return state.name === WorkflowDefinitionGraphApi.selectedNode;
    }

    /**
     * Output:
     * [
     *  {
     *    key: 'Category1Name',
     *    values: [
     *      {label: 'item1', value: 7},
     *      {label: 'item2', value: 2},
     *      ....
     *    ]
     *   },{
     *    key: 'Category2Name',
     *    values: [
     *      {label: 'item1', value: 3},
     *      {label: 'item2', value: 92},
     *      ....
     *    ]
     *   }, ...
     *  ]
     *
     *  Number of rows in values must be equal in all categories.
     *  Labels must be same and in same order in all categories.
     */
    function statsToData(definition, stats) {
      console.log('da process', stats);
      var data = {};
      // TODO add states from definition
      var allStateNames = Object.keys(stats.stateStatistics);
      // TODO read from data, add missing values from this list, skip finished, init data with these
      var allStatusNames = ['created', 'executing', 'paused', 'stopped', 'manual'];
      _.forEach(allStatusNames, function(statusName) {
        if(!data[statusName]) {
          data[statusName] = {
            key: _.capitalize(statusName),
            values: _.map(allStateNames, function(state) {
              return {
                label: state,
                value: 0,
              };
            }),
          };
        }
      });
      _.forEach(stats.stateStatistics, function(x, stateName) {
        var statusName = Object.keys(x)[0];
        if(!_.contains(allStatusNames, statusName)) {
          return;
        }
        var allInstances = x[statusName].allInstances;
        var queuedInstances = x[statusName].queuedInstances;
        //console.log(stateName, statusName, allInstances, queuedInstances  );
        var valueForStatus = _.find(data[statusName].values, {label: stateName});
        valueForStatus.value = allInstances + queuedInstances;
      });
      return _.values(data);
    }
    function updateStateExecutionGraph(type) {
      var stats = WorkflowStatsPoller.getLatest(type);
      if (!self.definition) {
        console.debug('Definition not loaded yet');
        return;
      }
      if (stats) {
        processStats(self.definition, stats);
        if(Object.keys(stats).length === 0) {
          return;
        }
        self.data = statsToData(self.definition, stats);
      }
    }

    // TODO move to service
    // TODO needed for All instances table
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
