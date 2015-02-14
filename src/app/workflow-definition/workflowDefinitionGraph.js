(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflowDefinition.graph', []);

  m.directive('workflowDefinitionGraph', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '='
      },
      bindToController: true,
      controller: 'WorkflowDefinitionGraphCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow-definition/workflowDefinitionGraph.html'
    };
  });

  m.controller('WorkflowDefinitionGraphCtrl', function($rootScope, $scope, SelectedNodeNotifier) {

    // store initial graph aspect ratio
    var dagreSvgSelector = '#dagreSvg';
    var aspectRatio = $(dagreSvgSelector).width() / $(dagreSvgSelector).height();

    var self = this;
    self.graph = undefined; // TODO no need to expose in view model?
    self.selectedNode = undefined; // TODO no need to expose in view model?
    self.nodeSelected = nodeSelected;
    self.savePng = savePng;
    self.saveSvg = saveSvg;

    initialize();

    function initialize() {
      self.graph = workflowDefinitionGraph(self.definition);

      var start = new Date().getTime();
      drawWorkflowDefinition(self.graph, dagreSvgSelector, nodeSelectedCallBack, $rootScope.graph.css);
      console.debug('Rendering dagre graph took', (new Date().getTime() - start), 'ms');

      SelectedNodeNotifier.addListener({ onSelectNode: nodeSelectedCallBack });
    }

    /** called when node is clicked or by SelectedNodeNotifier */
    function nodeSelected(nodeId) {
      console.debug('Selecting node ' + nodeId);
      if (self.selectedNode) {
        unhiglightNode(self.graph, self.definition, self.selectedNode);
      }
      if (nodeId) {
        higlightNode(self.graph, self.definition, nodeId);
      }
      self.selectedNode = nodeId;
    }

    // must use $apply() - event not managed by angular
    function nodeSelectedCallBack(nodeId) {
      $scope.$apply(function () {
        nodeSelected(nodeId);
      });
    }

    // download buttons
    function svgDataUrl() {
      var html = d3.select(dagreSvgSelector)
        .attr('version', 1.1)
        .attr('xmlns', 'http://www.w3.org/2000/svg')
        .node().outerHTML;
      return 'data:image/svg+xml;base64,' + btoa(html);
    }

    function downloadSvg(filename) {
      downloadDataUrl(svgDataUrl(), filename);
    }

    // TODO save as PNG doesn't work. due to css file?
    function savePng() {
      console.info('Save PNG');
      var selectedNode = self.selectedNode;
      nodeSelected(null);
      var h = $(dagreSvgSelector).height();
      var size = [h * aspectRatio, h];
      downloadImage(size, svgDataUrl(), self.definition.type + '.png', 'image/png');
      nodeSelected(selectedNode);
    }

    function saveSvg() {
      console.info('Save SVG');
      var selectedNode = self.selectedNode;
      nodeSelected(null);
      downloadSvg(self.definition.type + '.svg');
      nodeSelected(selectedNode);
    }

  });

})();
