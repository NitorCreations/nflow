'use strict';
angular.module('nflowVisApp.workflow_stats', [])
.controller('WorkflowStatsCtrl', function WorkflowStatsCtrl($scope, $rootScope, $interval, WorkflowDefinitions, WorkflowDefinitionStats,
                                                             $routeParams, config) {
  $scope.type=$routeParams.type;

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

  function getCurrentStateNames(data) {
    return _.reduce(data, function(acc, stats) {
      return _.map(stats[1], function(stat, stateName) {
        return stateName;
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

  ///////////////////////
  function createStateData(currentStates) {
    var data = $rootScope.radiator.stateChart.data;

    var dataArray = _.map(data, function(row) {
      var time = row[0];
      var stats = row[1];
      var values = _.map(currentStates, function(stateName) {
        if(!stats) {
          return undefined;
        }
        var stateStats = stats[stateName];
        if(!stateStats) {
          return 0;
        }
        return sum(_.values(stateStats));
      });

      return [time].concat(values);
    });

    return {dataArray: dataArray, labels: currentStates};
  }

  //
  function createExecutionData(currentStates) {
    var data = $rootScope.radiator.stateChart.data;
    var executionPhases = ['executing', 'nonScheduled', 'queued', 'sleeping'];

    var dataArray = _.map(data, function(row) {
      var time = row[0];
      var stats = row[1];
      var values = _.map(executionPhases, function(phase) {
        return sum(_.map(currentStates, function(stateName) {
          if(!stats) {
            return undefined;
          }
          var stateStats = stats[stateName];
          if(!stateStats) {
            return 0;
          }
          return stateStats[phase];
        }));
      });
      return [time].concat(values);
    });

    return {dataArray: dataArray, labels: executionPhases};
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
        xAxisLabelWidth: 55,
        responsive: true,
        stackedGraph: true,
        legend: 'always',
        //showRangeSelector: true,
        labelsDiv: canvasId + 'Legend',
        labelsSeparateLines: true,
        labels: labels
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

  function filterNonFinalStates(stateNames) {
    return _.filter(stateNames, function(stateName) {
      var state = _.first(_.filter($scope.definition.states, function(state) {
        return state.name === stateName;
      }));
      if(!state) {
        return true;
      }
      return state.type !== 'end';
    });
  }
  function draw() {
    // this loops over all data to get state names. maybe slow
    var currentStates = getCurrentStateNames($rootScope.radiator.stateChart.data);
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
