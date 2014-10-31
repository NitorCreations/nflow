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
        non_scheduled: 10
      };


      return result;
    });

    var stateStatMap = _.reduce(stateStatList, function(a, b) {
      return _.merge(a,b);
    });
    return {state_statistics: stateStatMap};
  }

  function loadStats(definition) {
    var stats = getStats(definition);

    _.each(definition.states, function(state) {
      var name = state.name;

      state.statistics = stats.state_statistics[name]
      state.statistics.total_active = _.reduce(_.values(state.statistics), function(a,b) {return a+b})
                  - state.statistics.non_scheduled;
    });
  };

  $scope.nodeSelected = nodeSelected;
  $scope.stats = {};
  WorkflowDefinitions.get({type: $routeParams.type},
                          function(data) {
                            var start = new Date().getTime();
                            var definition =  _.first(data);
                            $scope.definition = definition;

                            loadStats(definition);


                            $scope.graph = workflowDefinitionGraph(definition);
                            // must use $apply() - event not managed by angular
                            function nodeSelectedCallBack(nodeId) {
                              $scope.$apply(function() {
                                nodeSelected(nodeId);
                              });
                            }
                            drawWorkflowDefinition($scope.graph, 'dagreSvg', nodeSelectedCallBack);
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

