(function () {
  'use strict';

  var m = angular.module('nflowExplorer.executors', [
    'nflowExplorer.executors.executorTable',
    'nflowExplorer.services'
  ]);

  m.controller('ExecutorsCtrl', function ExecutorsCtrl(ExecutorService) {
    var self = this;
    self.executors = ExecutorService.executors;
  });

})();
