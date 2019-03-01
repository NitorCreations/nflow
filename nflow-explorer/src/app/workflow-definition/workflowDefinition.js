(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflowDefinition', [
    'nflowExplorer.workflowDefinition.tabs'
  ]);

  m.controller('WorkflowDefinitionCtrl', function (definition, config) {
    var self = this;
    self.definition = definition;
    self.contentGenerator = config.customDefinitionContent;
  });

})();
