(function () {
  'use strict';

  var m = angular.module('nflowExplorer.executors', [
    'nflowExplorer.executors.executorTable',
    'nflowExplorer.services.ExecutorPoller'
  ]);

  m.controller('ExecutorsCtrl', function ExecutorsCtrl(ExecutorPoller) {
    var self = this;
    self.executors = ExecutorPoller.executors;
  });

})();
