(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflow', [
    'nflowVisApp.workflow.graph',
    'nflowVisApp.workflow.info',
    'nflowVisApp.workflow.tabs'
  ]);

  m.controller('WorkflowCtrl', function (workflow, definition) {
    var self = this;
    self.workflow = workflow;
    self.definition = definition;
  });
})();
