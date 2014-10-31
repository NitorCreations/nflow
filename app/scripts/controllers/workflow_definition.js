'use strict';
/**
 * Display single workflow definition
 */
var app = angular.module('nflowVisApp');

app.factory('WorkflowDefinitions', function ($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-definition',
                   {type: '@type'},
                   {'get': {isArray: true}});
});

app.controller('WorkflowDefinitionCtrl', function ($scope, WorkflowDefinitions, $routeParams) {

  /** called when node is clicked */
  function nodeSelected(nodeId) {
    console.debug('Selecting node ' + nodeId);
    if($scope.selectedNode) {
      unhiglightNode($scope.graph, $scope.definition, $scope.selectedNode);
    }
    if(nodeId) {
      higlightNode($scope.graph, $scope.definition, nodeId);
    }
    $scope.selectedNode = nodeId;
  }

  // TODO replace with actual fetch
  function getStats(definition) {
    var stateStatList = _.map(definition.states, function(state) {
      var result = {};
      result[state.name] = {
        executing: state.name.length,
        queued: Math.floor(Math.random() * (10  + 1)),
        sleeping: 2,
        nonScheduled: 10
      };
      return result;
    });

    var stateStatMap = _.reduce(stateStatList, function(a, b) {
      return _.merge(a,b);
    });
    return {stateStatistics: stateStatMap};
  }

  function loadStats(definition) {
    var stats = getStats(definition);
    var totals = {executing: 0,
                  queued: 0,
                  sleeping: 0,
                  nonScheduled: 0,
                  totalActive: 0};

    _.each(definition.states, function(state) {
      var name = state.name;

      state.stateStatistics = stats.stateStatistics[name];

      state.stateStatistics.totalActive = _.reduce(_.values(state.stateStatistics), function(a,b) {
        return a+b;
      }, 0) - (state.stateStatistics.nonScheduled ? state.stateStatistics.nonScheduled : 0);
      _.each(['executing','queued','sleeping','nonScheduled', 'totalActive'], function(stat) {
        totals[stat] += state.stateStatistics[stat] ? state.stateStatistics[stat] : 0;
      });
    });
    definition.stateStatisticsTotal = totals;
    return stats;
  }

  $scope.nodeSelected = nodeSelected;
  $scope.stats = {};
  WorkflowDefinitions.get({type: $routeParams.type},
                          function(data) {
                            var start = new Date().getTime();
                            var definition =  _.first(data);
                            $scope.definition = definition;

                            var stats = loadStats(definition);
                            console.info(stats);
                            $scope.graph = workflowDefinitionGraph(definition);
                            // must use $apply() - event not managed by angular
                            function nodeSelectedCallBack(nodeId) {
                              $scope.$apply(function() {
                                nodeSelected(nodeId);
                              });
                            }
                            drawWorkflowDefinition($scope.graph, 'dagreSvg', nodeSelectedCallBack);
                            drawStateExecutionGraph('statisticsGraph', stats.stateStatistics);
                            console.debug('Rendering dagre graph took ' +
                                          (new Date().getTime() - start) + ' msec' );
                          });

  function svgDataUrl() {
    var html = d3.select('#dagreSvg')
      .attr('version', 1.1)
      .attr('xmlns', 'http://www.w3.org/2000/svg')
      .node().outerHTML;
    return 'data:image/svg+xml;base64,'+ btoa(html);
  }


  function downloadSvg(filename) {
    downloadDataUrl(svgDataUrl(), filename);
  }

  $scope.savePng = function savePng() {
    console.log('Save PNG');
    var selectedNode = $scope.selectedNode;
    nodeSelected(null);
    downloadImage(svgDataUrl(), $scope.definition.type + '.png', 'image/png');
    nodeSelected(selectedNode);
  };

  $scope.saveSvg = function saveSvg() {
    console.log('Save SVG');
    var selectedNode = $scope.selectedNode;
    nodeSelected(null);
    downloadSvg($scope.definition.type + '.svg');
    nodeSelected(selectedNode);
  };

  $scope.prettyPrintJson = function(value) {
    try {
      return JSON.stringify(value, undefined, 2);
    } catch(e) {
      return value;
    }
  };
});

