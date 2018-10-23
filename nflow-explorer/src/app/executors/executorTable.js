(function () {
  'use strict';

  var m = angular.module('nflowExplorer.executors.executorTable', [
    'nflowExplorer.services',
    'nflowExplorer.components',
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
      templateUrl: 'app/executors/executorTable.html'
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
      now = now || Time.currentMoment();
      if (executor.stopped) {
        return;
      }

      if (!executor.expires) { // has never been active

        if (moment(executor.started).add(1, 'days').isBefore(now)) { // dead
          return;
        }
        if (moment(executor.started).add(1, 'hours').isBefore(now)) { // expired
          return 'warning';
        }
        return 'success'; // alive
      }

      // has been active

      if (moment(executor.active).add(1, 'days').isBefore(now)) { // dead
        return;
      }
      if (moment(executor.expires).isBefore(now)) { // expired
        return 'warning';
      }
      return 'success'; // alive
    }
  });

})();
