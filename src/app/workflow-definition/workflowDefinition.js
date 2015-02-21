(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflowDefinition', [
    'nflowVisApp.workflowDefinition.graph',
    'nflowVisApp.workflowDefinition.tabs'
  ]);

  m.controller('WorkflowDefinitionCtrl', function (definition) {
    var self = this;
    self.definition = definition;
  });

})();
