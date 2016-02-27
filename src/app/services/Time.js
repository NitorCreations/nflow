(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.Time', []);

  m.service('Time', function Time() {
    var self = this;
    self.currentMoment = function () {
      return moment();
    };
  });

})();
