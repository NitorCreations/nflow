(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflowDefinition.graph', [
    'nflowExplorer.graph'
  ]);

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

  m.controller('WorkflowDefinitionGraphCtrl', function($rootScope, $scope, WorkflowDefinitionGraphApi, Graph) {
    var svg;
    var graph;

    var self = this;
    self.savePng = savePng;
    self.saveSvg = saveSvg;

    initialize();

    function initialize() {
      svg = initSvg();
      graph = initGraph(self.definition);

      WorkflowDefinitionGraphApi.initialize(graph.nodeSelected);

      var start = new Date().getTime();
      graph.drawWorkflowDefinition();
      console.debug('Rendering dagre graph took', (new Date().getTime() - start), 'ms');
    }

    function savePng() {
      console.info('Save PNG');
      graph.save(function() { Graph.downloadImage(svg.size(), svg.dataUrl(), self.definition.type + '.png', 'image/png'); });
    }

    function saveSvg() {
      console.info('Save SVG');
      graph.save(function() { Graph.downloadDataUrl(svg.dataUrl(), self.definition.type + '.svg'); });
    }

    function initSvg() {
      var selector = '#dagreSvg';
      var aspectRatio = $(selector).width() / $(selector).height();

      var self = {};
      self.selector = selector;

      self.dataUrl = function() {
        var html = d3.select(selector)
          .attr('version', 1.1)
          .attr('xmlns', 'http://www.w3.org/2000/svg')
          .node().outerHTML;
        return 'data:image/svg+xml;base64,' + btoa(html);
      };

      self.size = function() {
        var h = $(selector).height();
        return [h * aspectRatio, h];
      };

      return self;
    }

    function initGraph(definition) {
      var d = definition;
      var g = Graph.workflowDefinitionGraph(d);

      var self = {};

      self.drawWorkflowDefinition = function() {
        Graph.drawWorkflowDefinition(g, svg.selector, WorkflowDefinitionGraphApi.onSelectNode, $rootScope.graph.css);
      };

      self.nodeSelected = function(nodeId) {
        var previouslySelectedNode = WorkflowDefinitionGraphApi.selectedNode;
        console.debug('Selecting node ' + nodeId);
        if (previouslySelectedNode) { Graph.unhighlightNode(g, d, previouslySelectedNode); }
        if (nodeId) { Graph.highlightNode(g, d, nodeId); }
      };

      self.save = function(saveFn) {
        var nodeToRestore = WorkflowDefinitionGraphApi.selectedNode;
        self.nodeSelected(null);
        saveFn();
        self.nodeSelected(nodeToRestore);
      };

      return self;
    }

  });

  m.factory('WorkflowDefinitionGraphApi', function($timeout) {
    var onSelectNodeFn = _.noop;

    var api = {};
    api.initialize = initialize;
    api.onSelectNode = onSelectNode;
    api.selectedNode = undefined;
    return api;

    function initialize(onSelectNodeFnToBind) { onSelectNodeFn = onSelectNodeFnToBind; }
    function onSelectNode(nodeId) {
      // TODO Graph.drawWorkflowDefinition should encapsulate handling on non-angular events, remove $timeout
      // from here when Graph has been refactored.
      $timeout(function() {
        onSelectNodeFn(nodeId);
        api.selectedNode = nodeId;
      });
    }
  });

})();
