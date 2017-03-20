(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflowDefinition', [
    'nflowExplorer.workflowDefinition.tabs'
  ]);

  m.controller('WorkflowDefinitionCtrl', function (definition) {
    var self = this;
    self.definition = definition;
  });

})();
