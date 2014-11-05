'use strict';
angular.module('nflowVisApp.radiator', [])
.controller('RadiatorCtrl', function ($scope, $rootScope, $interval, WorkflowDefinitions, WorkflowDefinitionStats,
                                       $routeParams, config) {
  $scope.type=$routeParams.type;

  var itemCount = config.maxHistorySize;
  if(!$rootScope.radiator) {
    $rootScope.radiator = {};
    $rootScope.radiator.stateChart = {};
    $rootScope.radiator.stateChart.data = [];
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
  function createStateData() {
    var data = $rootScope.radiator.stateChart.data;

    // TODO this loops over all data to get state names
    var currentStates = getCurrentStateNames(data);

    var dataArray = _.map(data, function(row) {
      var time = row[0];
      var stats = row[1];
      var values = _.map(currentStates, function(stateName) {
        if(!stats) {
          return undefined;
        }
        var stateStats = stats[stateName];
        if(!stateStats) {
          return undefined;
        }
        return sum(_.values(stateStats));
      });

      return [time].concat(values);
    });

    return {dataArray: dataArray, labels: currentStates};
  };

  //
  function createExecutionData() {
    var data = $rootScope.radiator.stateChart.data;

    var executionPhases = ['executing', 'nonScheduled', 'queued', 'sleeping'];
    var currentStates = getCurrentStateNames(data);

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
  };

  function drawStackedLineChart(canvasId, data) {
    var canvas = document.getElementById(canvasId);
    if(!canvas) {
      return;
    }

    var options = {
      showRoller: true,
      responsive: true,
      stackedGraph: true,
      legend: 'always',
      labels: ['timestamp'].concat(data.labels)
    };

    new Dygraph(canvas, data.dataArray, options);
  }

  //
  function addStateData(time, stats) {
    var d = $rootScope.radiator.stateChart.data;
    d.push([time, stats]);
    while(d.length > itemCount) {
      d.shift();
    }
  }

  function updateChart() {
    WorkflowDefinitionStats.get({type: $scope.type},
                                function(stats) {
                                  console.log('stats', stats);
                                  addStateData(new Date(), stats.stateStatistics);
                                  drawStackedLineChart('stateChart', createStateData());
                                  drawStackedLineChart('executionChart', createExecutionData());
                                },
                                function(error) {
                                  console.log(error);
                                  addStateData(new Date(), {});
                                  drawStackedLineChart('stateChart', createStateData());
                                  drawStackedLineChart('executionChart', createExecutionData());
                                });
  }
  updateChart();

  if(!$rootScope.radiator.radiatorStatsTask) {
    $rootScope.radiator.radiatorStatsTask = $interval(updateChart, config.radiator.pollPeriod *1000);
  }

});
