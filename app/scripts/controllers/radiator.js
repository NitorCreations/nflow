'use strict';
angular.module('nflowVisApp.radiator', [])
.controller('RadiatorCtrl', function ($scope, $rootScope, $interval, WorkflowDefinitions, WorkflowDefinitionStats, $routeParams) {
  $scope.name=$routeParams.type;

  var itemCount = 10000;
  if(!$rootScope.radiator) {
    $rootScope.radiator = {};
    $rootScope.radiator.stateChart = {};
    $rootScope.radiator.stateChart.data = [];
  }



  ///////////////////////
  function createStateData() {
    var data = $rootScope.radiator.stateChart.data;

    var chartData = {};
    chartData.datasets = [];
    var currentStates = _.reduce(data, function(acc, stats) {
      return _.map(stats[1], function(stat, stateName) {
        return stateName;
      });
    }, {}).sort();


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
        var nonZeroValues = _.compact(_.values(stateStats));
        return _.reduce(nonZeroValues, function(acc, value){
          return acc+value;
        }, 0);
      });

      return [time].concat(values);
    });

    return {dataArray: dataArray, labels: currentStates};

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
    WorkflowDefinitionStats.get({type: $routeParams.type},
                                function(stats) {
                                  console.log('stats', stats);
                                  addStateData(new Date(), stats.stateStatistics);
                                  var stateData = createStateData();
                                  drawStackedLineChart('stateChart', stateData);
                                },
                                function(error) {
                                  console.log(error);
                                  addStateData(new Date(), {});
                                  drawStackedLineChart('stateChart');
                                });
  }
  updateChart();

  if(!$rootScope.radiator.radiatorStatsTask) {
    $rootScope.radiator.radiatorStatsTask = $interval(updateChart, 5000);
  }

});
