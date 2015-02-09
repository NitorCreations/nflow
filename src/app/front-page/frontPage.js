(function () {
  'use strict';

  var m = angular.module('nflowVisApp.frontPage', [
    'nflowVisApp.frontPage.definitionList',
    'nflowVisApp.frontPage.executorTable',
    'nflowVisApp.services'
  ]);

  m.controller('FrontPageCtrl', function FrontPageCtrl(WorkflowDefinitions) {
    var vm = this;
    vm.definitions = WorkflowDefinitions.query();
  });

})();
