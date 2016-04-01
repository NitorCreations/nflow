(function () {
  'use strict';

  var m = angular.module('nflowExplorer.frontPage', [
    'nflowExplorer.frontPage.definitionList',
    'nflowExplorer.services',
  ]);

  m.controller('FrontPageCtrl', function FrontPageCtrl(WorkflowDefinitionService) {
    var self = this;

    WorkflowDefinitionService.list()
      .then(function(definitions) {
        self.definitions = definitions;
      });
  });

})();
