(function () {
  'use strict';

  var m = angular.module('nflowVisApp.frontPage', [
    'nflowVisApp.frontPage.definitionList',
    'nflowVisApp.frontPage.executorTable',
    'nflowVisApp.services',
    'nflowVisApp.services.executorPoller'
  ]);

  m.controller('FrontPageCtrl', function FrontPageCtrl(WorkflowDefinitions, ExecutorPoller) {
    var self = this;
    self.definitions = WorkflowDefinitions.query();
    self.executors = ExecutorPoller.executors;
  });

})();
