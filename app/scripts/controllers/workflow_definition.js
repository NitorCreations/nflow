'use strict';
/**
 * Display single workflow definition
 */
angular.module('nflowVisApp')
.controller('WorkflowDefinitionCtrl', function ($scope, WorkflowDefinitions, WorkflowDefinitionStats, $routeParams) {

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

  // TODO move to service
  function processStats(definition, stats) {
    var totals = {executing: 0,
                  queued: 0,
                  sleeping: 0,
                  nonScheduled: 0,
                  totalActive: 0};
    if(!stats.stateStatistics) {
      stats.stateStatistics = {};
    }
    _.each(definition.states, function(state) {
      var name = state.name;

      state.stateStatistics = stats.stateStatistics[name] ? stats.stateStatistics[name] : {};

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

  $scope.hasStatistics = false;
  $scope.nodeSelected = nodeSelected;

  // TODO handle errors
  WorkflowDefinitions.get({type: $routeParams.type},
                          function(data) {
                            var start = new Date().getTime();
                            var definition =  _.first(data);
                            $scope.definition = definition;


                            //var stats = loadStats(definition);
                            $scope.graph = workflowDefinitionGraph(definition);
                            // must use $apply() - event not managed by angular
                            function nodeSelectedCallBack(nodeId) {
                              $scope.$apply(function() {
                                nodeSelected(nodeId);
                              });
                            }
                            drawWorkflowDefinition($scope.graph, 'dagreSvg', nodeSelectedCallBack);

                            // TODO handle errors
                            WorkflowDefinitionStats.get({type: $routeParams.type},
                                                       function(stats) {
                                                         processStats(definition, stats);
                                                         console.info(stats);
                                                         $scope.hasStatistics = drawStateExecutionGraph('statisticsGraph', stats.stateStatistics, definition, nodeSelectedCallBack);
                                                       });

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

  // TODO move to $rootScope
  $scope.prettyPrintJson = function(value) {
    try {
      return JSON.stringify(value, undefined, 2);
    } catch(e) {
      return value;
    }
  };
});

