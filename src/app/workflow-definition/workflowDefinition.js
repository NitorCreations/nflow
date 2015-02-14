(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflowDefinition', []);

  m.controller('WorkflowDefinitionCtrl', function WorkflowDefinitionCtrl($scope, $rootScope, $routeParams,
                                                                         WorkflowDefinitions, WorkflowDefinitionStats, WorkflowStatsPoller) {
    /** called when node is clicked */
    function nodeSelected(nodeId) {
      console.debug('Selecting node ' + nodeId);
      if ($scope.selectedNode) {
        unhiglightNode($scope.graph, $scope.definition, $scope.selectedNode);
      }
      if (nodeId) {
        higlightNode($scope.graph, $scope.definition, nodeId);
      }
      $scope.selectedNode = nodeId;
    }

    $scope.startRadiator = function () {
      $rootScope.$broadcast('startRadiator');
    };

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

    $scope.hasStatistics = false;
    $scope.nodeSelected = nodeSelected;

    // must use $apply() - event not managed by angular
    function nodeSelectedCallBack(nodeId) {
      $scope.$apply(function () {
        nodeSelected(nodeId);
      });
    }

    function updateStateExecutionGraph(type) {
      var stats = WorkflowStatsPoller.getLatest(type);
      if (!$scope.definition) {
        console.debug('Definition not loaded yet');
        return;
      }
      if (stats) {
        processStats($scope.definition, stats);
        $scope.hasStatistics = drawStateExecutionGraph('statisticsGraph', stats.stateStatistics, $scope.definition, nodeSelectedCallBack);
      }
    }

    // TODO handle errors
    WorkflowDefinitions.get({type: $routeParams.type},
      function (data) {
        var start = new Date().getTime();
        var definition = _.first(data);
        $scope.definition = definition;
        $scope.graph = workflowDefinitionGraph(definition);
        drawWorkflowDefinition($scope.graph, 'dagreSvg', nodeSelectedCallBack, $rootScope.graph.css);
        updateStateExecutionGraph($routeParams.type);
        console.debug('Rendering dagre graph took ' +
        (new Date().getTime() - start) + ' msec');
      });

    // poller polls stats with fixed period
    WorkflowStatsPoller.start($routeParams.type);

    // poller broadcasts when events change
    $scope.$on('workflowStatsUpdated', function (scope, type) {
      if (type !== $routeParams.type) {
        return;
      }
      updateStateExecutionGraph(type);

    });

    // download buttons
    function svgDataUrl() {
      var html = d3.select('#dagreSvg')
        .attr('version', 1.1)
        .attr('xmlns', 'http://www.w3.org/2000/svg')
        .node().outerHTML;
      return 'data:image/svg+xml;base64,' + btoa(html);
    }

    function downloadSvg(filename) {
      downloadDataUrl(svgDataUrl(), filename);
    }

    // TODO save as PNG doesn't work. due to css file?
    $scope.savePng = function savePng() {
      console.info('Save PNG');
      var selectedNode = $scope.selectedNode;
      nodeSelected(null);
      downloadImage(svgDataUrl(), $scope.definition.type + '.png', 'image/png');
      nodeSelected(selectedNode);
    };

    $scope.saveSvg = function saveSvg() {
      console.info('Save SVG');
      var selectedNode = $scope.selectedNode;
      nodeSelected(null);
      downloadSvg($scope.definition.type + '.svg');
      nodeSelected(selectedNode);
    };

  });

})();
