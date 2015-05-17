(function () {
  'use strict';

  var m = angular.module('nflowExplorer.executors', [
    'nflowExplorer.executors.executorTable',
    'nflowExplorer.services.executorPoller'
  ]);

  m.controller('ExecutorsCtrl', function ExecutorsCtrl(ExecutorPoller) {
    var self = this;
    self.executors = ExecutorPoller.executors;
  });

})();
