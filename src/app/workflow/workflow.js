(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow', [
    'nflowExplorer.workflow.graph',
    'nflowExplorer.workflow.info',
    'nflowExplorer.workflow.tabs'
  ]);

  m.controller('WorkflowCtrl', function (workflow, parentWorkflow, definition) {
    var self = this;
    self.workflow = workflow;
    self.parentWorkflow = parentWorkflow;
    self.definition = definition;
  });
})();
