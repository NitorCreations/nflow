(function () {
  'use strict';
  var _statusNames = ['created', 'inProgress', 'executing', 'finished', 'manual'];
  var _metaStatuses = ['queued', 'sleeping', 'executing', 'manual'];

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

  m.controller('WorkflowDefinitionTabsCtrl', function($rootScope, $scope, WorkflowDefinitionGraphApi, WorkflowDefinitionStats, WorkflowStatsPoller, $timeout) {
    var self = this;
    self.hasStatistics = false;
    self.selectNode = WorkflowDefinitionGraphApi.onSelectNode;
    self.isStateSelected = isStateSelected;
    self.startRadiator = startRadiator;

    var width = 450, height = 400;
    self.options = {chart: {
      // multiBarHorizontalChart get ugly colors due to https://github.com/novus/nvd3/issues/916
                type: 'multiBarChart',
                callback: function(chart) {
                  chart.container.setAttribute('preserveAspectRatio', 'xMinYMin');
                  var c = Math.min(width, height);
                  chart.container.setAttribute('viewBox', '0 0 '+ c +' ' + c );
                  $timeout(function(){
                    // TODO: kludge workaround for https://github.com/krispo/angular-nvd3/issues/100
                    chart.stacked(true);
                    chart.update();
                  });
                },
                height: height,
                width: width,
                margin : {
                    top: 20,
                    right: 20,
                    bottom: 160,
                    left: 45
                },
                noData: 'No workflow instances in active states. There may be finished instances.',
                x: function(d) { return d.label; },
                y: function(d) { return d.value; },
                clipEdge: true,
                staggerLabels: false,
                reduceXTicks: false,
                showControls: true,
                duration: 0,
                transitionDuration: 0,
                // TODO this doesn't work
                stacked: true,
                xAxis: {
                    showMaxMin: false,
                    rotateLabels: 25,
                },
                yAxis: {
                    axisLabel: 'Workflow instances',
                    axisLabelDistance: 40,
                    tickFormat: function(d) {
                        return d3.format(',d')(d);
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
      return state.id === WorkflowDefinitionGraphApi.selectedNode;
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
      var definitionStateIds = _.map(definition.states, function(state) {
        return state.id;
      });
      var statsStateIds = Object.keys(stats.stateStatistics);
      // add any extra state present in stats, but not present in definition
      var allStateIds = definitionStateIds.concat(_.filter(statsStateIds, function(state) {
        return !_.contains(definitionStateIds, state);
      }));

      var activeStateIds = _.filter(allStateIds, function(stateId) {
        // remove states that are know to be end states
        var definitionState = _.find(definition.states, {id: stateId});
        if(!definitionState) {
          return true;
        }
        if(definitionState.type === 'end') {
          return false;
        }
        return true;
      });
      var allStatusNames = _metaStatuses;
      _.forEach(allStatusNames, function(statusName) {
        if(!data[statusName]) {
          data[statusName] = {
            key: _.startCase(statusName),
            values: _.map(activeStateIds, function(state) {
              return {
                label: state,
                value: 0,
              };
            }),
          };
        }
      });
      _.forEach(stats.stateStatistics, function(stateStats, stateId) {
        if(!_.contains(activeStateIds, stateId)) {
          return;
        }
        _.forEach(Object.keys(stateStats), function(statusName) {
          if(!_.contains(allStatusNames, statusName)) {
            return;
          }
          var valueForStatus = _.find(data[statusName].values, {label: stateId});
          valueForStatus.value = stateStats[statusName].allInstances || 0;
        });
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
     * Adds definition.stateStatisticsTotal and definition.states[stateId].stats
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
     * definition.states[stateId].stats = {
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
      _.forEach(definition.states, function (state) {
        var id = state.id;

        state.stateStatistics = stats.stateStatistics[id] || {};

        // TODO calculate correctly, remove non active
        state.stateStatistics.allInstances = _.reduce(_.values(state.stateStatistics), function (a, b) {
          return a + b.allInstances;
        }, 0);
        state.stateStatistics.queuedInstances = _.reduce(_.values(state.stateStatistics), function (a, b) {
          return a + b.queuedInstances || 0;
        }, 0);

        // calculate totals
        _.forEach(allStatusNames, function (stat) {
          if(state.stateStatistics[stat]) {
            var allInstances = state.stateStatistics[stat].allInstances || 0;
            var queuedInstances = state.stateStatistics[stat].queuedInstances || 0;
            totalStats[stat].allInstances += allInstances;
            totalStats[stat].queuedInstances += queuedInstances;
            totalStats.allInstances += allInstances;
            totalStats.queuedInstances += queuedInstances;
          }
        });
        function queued(stats) {
          if(!stats) {
            return 0;
          }
          return stats.queuedInstances;
        }
        function sleeping(stats) {
          if(!stats) {
            return 0;
          }
          return stats.allInstances - stats.queuedInstances;
        }

        state.stateStatistics.queued = {
          allInstances: queued(state.stateStatistics.created) +
            queued(state.stateStatistics.inProgress)
        };
        state.stateStatistics.sleeping = {
          allInstances: sleeping(state.stateStatistics.created) +
            sleeping(state.stateStatistics.inProgress)
        };
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
      return state.id === WorkflowDefinitionGraphApi.selectedNode;
    }
  });
})();
