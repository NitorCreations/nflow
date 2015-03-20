(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow.graph', []);

  m.directive('workflowGraph', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '=',
        workflow: '='
      },
      bindToController: true,
      controller: 'WorkflowGraphCtrl',
      controllerAs: 'ctrl',
      template: '<div class="svg-container"><svg id="workflowSvg"/></div>'
    };
  });

  m.controller('WorkflowGraphCtrl', function ($rootScope, WorkflowGraphApi) {
    var graph;
    var self = this;

    initialize();

    function initialize() {
      graph = initGraph(self.definition, self.workflow);

      WorkflowGraphApi.registerOnSelectNodeListener(graph.nodeSelected);

      graph.drawWorkflowDefinition();
      markCurrentState(self.workflow);
    }

    function initGraph(definition, workflow) {
      var d = definition;
      var w = workflow;
      var g = workflowDefinitionGraph(d, w);
      var selectedNode;

      var self = {};

      self.drawWorkflowDefinition = function() {
        drawWorkflowDefinition(g, '#workflowSvg', WorkflowGraphApi.onSelectNode, $rootScope.graph.css);
      };

      self.nodeSelected = function(nodeId) {
        console.debug('Selecting node ' + nodeId);
        if (selectedNode) { unhiglightNode(g, d, selectedNode, w); }
        if (nodeId) { higlightNode(g, d, nodeId, w); }
        selectedNode = nodeId;
      };

      return self;
    }
  });

  m.factory('WorkflowGraphApi', function() {
    var onSelectNodeListeners = [];

    var api = {};
    api.onSelectNode = onSelectNode;
    api.registerOnSelectNodeListener = registerOnSelectNodeListener;
    return api;

    function onSelectNode(nodeId) {
      _.forEach(onSelectNodeListeners, function(fn) { fn(nodeId); });
    }

    function registerOnSelectNodeListener(fn) { onSelectNodeListeners.push(fn); }
  });
})();
