(function () {
  'use strict';

  var m = angular.module('nflowExplorer.frontPage', [
    'nflowExplorer.frontPage.definitionList',
    'nflowExplorer.frontPage.executorTable',
    'nflowExplorer.services',
    'nflowExplorer.services.executorPoller'
  ]);

  m.controller('FrontPageCtrl', function FrontPageCtrl(WorkflowDefinitions, ExecutorPoller) {
    var self = this;
    self.definitions = WorkflowDefinitions.query();
    self.executors = ExecutorPoller.executors;
  });

})();
