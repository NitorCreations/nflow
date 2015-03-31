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

  m.controller('ExecutorTableCtrl', function (Time) {
    var self = this;
    self.executorClass = executorClass;
    self.isActive = isActive;

    function isActive(executor) {
      var passiveLimit = Time.currentMoment().add(-24, 'hours');
      if(executor.expires) {
        var expires = moment(executor.expires);
        if(expires.isBefore(passiveLimit)) {
          return false;
        }
      } else {
        return moment(executor.started).isAfter(passiveLimit);
      }
      return true;
    }

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
