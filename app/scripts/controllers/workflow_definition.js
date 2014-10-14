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
      unhiglightNode($scope.graph, $scope.workflow, $scope.selectedNode);
    }
    if(nodeId) {
      higlightNode($scope.graph, $scope.workflow, nodeId);
    }
    $scope.selectedNode = nodeId;
  }

  $scope.nodeSelected = nodeSelected;

  WorkflowDefinitions.get({type: $routeParams.type},
                          function(data) {
                            var start = new Date().getTime();
                            var definition =  _.first(data);
                            $scope.workflow = definition;
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
    downloadImage(svgDataUrl(), $scope.workflow.type + '.png', 'image/png');
    nodeSelected(selectedNode);
  };

  $scope.saveSvg = function saveSvg() {
    console.log('Save SVG');
    var selectedNode = $scope.selectedNode;
    nodeSelected(null);
    downloadSvg($scope.workflow.type + '.svg');
    nodeSelected(selectedNode);
  };
});

