(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow', [
    'nflowExplorer.workflow.graph',
    'nflowExplorer.workflow.info',
    'nflowExplorer.workflow.tabs'
  ]);

  m.controller('WorkflowCtrl', function (workflow, definition) {
    var self = this;
    self.workflow = workflow;
    self.definition = definition;
  });
})();
