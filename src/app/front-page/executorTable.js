(function () {
  'use strict';

  var m = angular.module('nflowExplorer.frontPage.executorTable', [
    'nflowExplorer.filters'
  ]);

  m.directive('executorTable', function () {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        executors: '='
      },
      bindToController: true,
      controller: 'ExecutorTableCtrl as ctrl',
      templateUrl: 'app/front-page/executorTable.html'
    };
  });

  m.controller('ExecutorTableCtrl', function () {
    var self = this;
    self.executorClass = executorClass;

    function executorClass(executor, now) {
      now = now || moment();
      var expires = moment(executor.expires);
      var active = moment(executor.active);
      if (active.add(1, 'days').isBefore(now)) {
        return;
      }
      if (expires.isBefore(now)) {
        return 'warning';
      }
      return 'success';
    }
  });

})();
