(function () {
  'use strict';
  var _statusNames = ['created', 'executing', 'inProgress', 'finished',
                      'manual', 'stopped', 'paused'];
  var m = angular.module('nflowExplorer.workflowDefinition.tabs', [
    'nvd3',
  ]);

  /** <workflow-definition-tabs></workflow-definition-tabs> */
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
                refreshDataOnly: true,
                height: 450,
                // TODO doesn't work currently
                preserveAspectRatio: 'xMinYMin',
                // TODO doesn't work currently
                viewBox: '0 0 100 100',
                width: 400,
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 160,
                    left: 45
                },
                noData: 'No workflow instances in non-final states.',
                x: function(d) { return d.label; },
                y: function(d) { return d.value; },
                clipEdge: true,
                staggerLabels: false,
                reduceXTicks: false,
                showControls: false,
                duration: 0,
                transitionDuration: 0,
                // TODO this doesn't work
                stacked: true,
                xAxis: {
                    axisLabel: 'States',
                    showMaxMin: false,
                    rotateLabels: 25,
                },
                yAxis: {
                    axisLabel: 'Workflow instances',
                    axisLabelDistance: 40,
                    tickFormat: function(d) {
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
     *    key: 'Status1Name',
     *    values: [
     *      {label: 'state1', value: 7},
     *      {label: 'state2', value: 2},
     *      ...
     *    ]
     *   },{
     *    key: 'Status2Name',
     *    values: [
     *      {label: 'state1', value: 3},
     *      {label: 'state2', value: 92},
     *      ...
     *    ]
     *   }, ...
     *  ]
     *
     *  Number of rows in values must be equal in all categories.
     *  Labels must be same and in same order in all categories.
     */
    function statsToData(definition, stats) {
      var data = {};
      // TODO add states from definition
      var allStateNames = Object.keys(stats.stateStatistics);
      // TODO read from data, add missing values from this list, skip finished, init data with these
      var allStatusNames = _.filter(_statusNames, function(name) {
        return name !== 'finished';
      });
      _.forEach(allStatusNames, function(statusName) {
        if(!data[statusName]) {
          data[statusName] = {
            key: _.startCase(statusName),
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
        var valueForStatus = _.find(data[statusName].values, {label: stateName});
        valueForStatus.value = x[statusName].allInstances || 0;
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

    /**
     * Compute list of all status names.
     * We want to show all statuses, including those that API doesn't return
     * and new statuses that API may return in future.
     */
    function getStatusNames(stats) {
      var names = [].concat(_statusNames);
      names.concat(_.flatten(_.map(stats, function(stat){
        return _.keys(stat);
      })));
      return _.uniq(names);
    }

    // TODO move to service
    /**
     * Output:
     *
     *
     * Modifies definition:
     * Adds definition.stateStatisticsTotal and definition.states[stateName].stats
     *
     * definition.stateStatisticsTotal = {
     *   totalInstances: 1231,
     *   totalQueued: 102,
     *   created: {
     *     allInstances: 91,
     *     queuedInstances: 29,
     *   },
     *   executing: {
     *     allInstances: 21,
     *     queuedInstances: 0,
     *   },
     *   ...
     * }
     *
     * definition.states[stateName].stats = {
     *   totalInstances: 10,
     *   queuedInstances: 3
     *   created: {
     *     allInstances: 4,
     *     queuedInstances: 3,
     *   },
     *   executing: {
     *     allInstances: 2,
     *     queuedInstances: 1,
     *   },
     *   ...
     * }
     */
    function processStats(definition, stats) {
      var allStatusNames = getStatusNames(stats);
      var totalStats = {allInstances: 0, queuedInstances: 0};
      _.forEach(allStatusNames, function(name) {
        totalStats[name] = {
          allInstances: 0,
          queuedInstances: 0,
        };
      });

      if (!stats.stateStatistics) {
        stats.stateStatistics = {};
      }
      _.each(definition.states, function (state) {
        var name = state.name;

        state.stateStatistics = stats.stateStatistics[name] || {};

        // TODO calculate correctly, remove non active
        state.stateStatistics.allInstances = _.reduce(_.values(state.stateStatistics), function (a, b) {
          return a + b.allInstances;
        }, 0);
        state.stateStatistics.queuedInstances = _.reduce(_.values(state.stateStatistics), function (a, b) {
          return a + b.queuedInstances;
        }, 0);

        // calculate totals
        _.each(allStatusNames, function (stat) {
          if(state.stateStatistics[stat]) {
            var allInstances = state.stateStatistics[stat].allInstances || 0;
            var queuedInstances = state.stateStatistics[stat].queuedInstances || 0;
            totalStats[stat].allInstances += allInstances;
            totalStats[stat].queuedInstances += queuedInstances;
            totalStats.allInstances += allInstances;
            totalStats.queuedInstances += queuedInstances;
          }
        });
      });
      definition.stateStatisticsTotal = totalStats;
      return stats;
    }

    function startRadiator() {
      $rootScope.$broadcast('startRadiator');
    }
  });


  /** <workflow-definition-tabs></workflow-definition-tabs> */
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
      templateUrl: 'app/workflow-definition/workflowStatisticsTable.html'
    };
  });
  m.controller('WorkflowStatisticsTable', function(WorkflowDefinitionGraphApi) {
    var self = this;
    self.isStateSelected = isStateSelected;
    self.selectNode = WorkflowDefinitionGraphApi.onSelectNode;

    function isStateSelected(state) {
      return state.name === WorkflowDefinitionGraphApi.selectedNode;
    }
  });
})();
