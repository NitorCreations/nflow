(function () {
  'use strict';

  var m = angular.module('nflowVisApp.frontPage', [
    'nflowVisApp.frontPage.definitionList',
    'nflowVisApp.frontPage.executorTable',
    'nflowVisApp.services',
    'nflowVisApp.services.executorPoller'
  ]);

  m.controller('FrontPageCtrl', function FrontPageCtrl(WorkflowDefinitions, ExecutorPoller) {
    var vm = this;
    vm.definitions = WorkflowDefinitions.query();
    vm.executors = ExecutorPoller.executors;
  });

})();
