(function () {
  'use strict';

  var m = angular.module('nflowExplorer.frontPage', [
    'nflowExplorer.frontPage.definitionList',
    'nflowExplorer.services',
  ]);

  m.controller('FrontPageCtrl', function FrontPageCtrl(WorkflowDefinitionService) {
    var self = this;

    console.log('jfjfjfjf', WorkflowDefinitionService.list())
    WorkflowDefinitionService.list().then(function(defs) {
      console.log('kheee', defs)
      self.definitions = defs;
    });
  });

})();
