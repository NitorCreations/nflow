'use strict';
angular.module('nflowExplorer.workflowStats', [])
.controller('WorkflowStatsCtrl', function WorkflowStatsCtrl($scope, $rootScope, $interval, WorkflowDefinitions, WorkflowDefinitionStats,
                                                             $stateParams, config) {
  $scope.type=$stateParams.type;

  var itemCount = config.maxHistorySize;
  $scope.definition = {};
  $scope.definition.states = [];

  if(!$scope.graphs) { $scope.graphs = {}; }

  function clearData() {
    if($rootScope.radiator && $rootScope.radiator.radiatorStatsTask) {
      $interval.cancel($rootScope.radiator.radiatorStatsTask);
    }
    $rootScope.radiator = {};
    $rootScope.radiator.type = $scope.type;
    $rootScope.radiator.stateChart = {};
    $rootScope.radiator.stateChart.data = [];
    $scope.graphs = {};
  }

  if(!$rootScope.radiator) {
    clearData();
  } else if ($rootScope.radiator.type !== $scope.type) {
    // workflow type was changed
    clearData();
  }

  function getCurrentStateIds(data) {
    return _.reduce(data, function(acc, stats) {
      return _.map(stats[1], function(stat, stateId) {
        return stateId;
      });
    }, {}).sort();
  }

  function sum(list) {
    return _.reduce(list, function(acc, value) {
      if(!value) {
        return acc;
      }
      return acc + value;
    }, 0);
  }

  /**
   * Output:
   * {
   *   dataArray: [
   *     timestamp, // Date
   *     workflowsInState1, // integer
   *     workflowsInState2,
   *     ...
   *   ],
   *   labels: [
   *     'state1Name',
   *     'state2Name',
   *     ...
   *   ]
   * }
   */
  function createStateData(currentStates) {
    var data = $rootScope.radiator.stateChart.data;

    var dataArray = _.map(data, function(row) {
      var time = row[0];
      var stats = row[1];
      var values = _.map(currentStates, function(stateId) {
        if(!stats) {
          return undefined;
        }
        var stateStats = stats[stateId];
        if(!stateStats) {
          return 0;
        }
        return sum(_.map(_.values(stateStats), function(s) {
          return s.allInstances;
        }));
      });

      return [time].concat(values);
    });

    var x = {dataArray: dataArray, labels: currentStates};
    return x;
  }

  /**
   * Output:
   * {
   *   dataArray: [
   *     timestamp, // Date
   *     workflowsInStatus1, // integer
   *     workflowsInStatus1,
   *     ...
   *   ],
   *   labels: [
   *     'status1Name',
   *     'status2Name',
   *     ...
   *   ]
   * }
   *
   */
  function createExecutionData(currentStates) {
    var data = $rootScope.radiator.stateChart.data;
    var executionPhases = ['queued', 'sleeping', 'executing', 'manual'];
    var realStatuses = ['executing', 'manual'];
    var dataArray = _.map(data, function(row) {
      var time = row[0],
          stats = row[1];
      var queued = 0,
          sleeping = 0;
      var values = _.map(realStatuses, function(phase) {
        return sum(_.map(currentStates, function(stateId) {
          if(!stats) {
            return undefined;
          }
          var stateStats = stats[stateId];
          if(!stateStats || !stateStats[phase]) {
            return 0;
          }
          return stateStats[phase].allInstances;
        }));
      });
      queued = sum(_.map(stats, function(s) {
        return s.created ? s.created.queuedInstances : 0 +
              s.inProgress ? s.inProgress.queuedInstances : 0;
      }));
      sleeping = sum(_.map(stats, function(s) {
        return s.created ? s.created.allInstances - s.created.queuedInstances : 0 +
              s.inProgress ? s.inProgress.allInstances - s.inProgress.queuedInstances : 0;
      }));
      return [time].concat([queued, sleeping]).concat(values);
    });
    return {dataArray: dataArray, labels: _.map(executionPhases, _.startCase)};
  }

  function drawStackedLineChart(canvasId, data) {
    var canvas = document.getElementById(canvasId);
    if(!canvas) {
      return;
    }

    var labels = ['timestamp'].concat(data.labels);

    if(!$scope.graphs[canvasId]) {
      var options = {
        axisLabelFontSize: 13,
        responsive: true,
        stackedGraph: true,
        legend: 'always',
        //showRangeSelector: true,
        labelsDiv: canvasId + 'Legend',
        labelsSeparateLines: true,
        labels: labels,
        axes: {
          x: {
            axisLabelWidth: 55,
          }
        },
      };
      $scope.graphs[canvasId] = new Dygraph(canvas, data.dataArray, options);
    } else {
      $scope.graphs[canvasId].updateOptions({file: data.dataArray, labels: labels});
    }
  }

  //
  function addStateData(time, stats) {
    var d = $rootScope.radiator.stateChart.data;
    d.push([time, stats]);
    while(d.length > itemCount) {
      d.shift();
    }
  }

  function filterNonFinalStates(stateIds) {
    return _.filter(stateIds, function(stateId) {
      var state = _.first(_.filter($scope.definition.states, function(state) {
        return state.id === stateId;
      }));
      if(!state) {
        return true;
      }
      return state.type !== 'end';
    });
  }
  function draw() {
    // this loops over all data to get state names. maybe slow
    var currentStates = getCurrentStateIds($rootScope.radiator.stateChart.data);
    currentStates = filterNonFinalStates(currentStates);
    drawStackedLineChart('stateChart', createStateData(currentStates));
    drawStackedLineChart('executionChart', createExecutionData(currentStates));
  }

  WorkflowDefinitions.get({type: $scope.type},
                          function(data) {
                            $scope.definition = _.first(data);
                          });


  function updateChart() {
    WorkflowDefinitionStats.get({type: $scope.type},
                                function(stats) {
                                  console.info('Fetching statistics', stats);
                                  addStateData(new Date(), stats.stateStatistics);
                                  draw();
                                },
                                function(error) {
                                  console.error(error);
                                  addStateData(new Date(), {});
                                  draw();
                                });
  }

  $scope.$on('$destroy', function(){
    // clear references to graphs when page unloads
    $scope.graphs = {};
  });

  $scope.$on('startRadiator', function () {
    updateChart();
    if(!$rootScope.radiator.radiatorStatsTask) {
      // start polling statistics
      $rootScope.radiator.radiatorStatsTask = $interval(updateChart, config.radiator.pollPeriod *1000);
    }
  });

});
