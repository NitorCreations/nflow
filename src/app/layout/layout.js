(function () {
  'use strict';

  var m = angular.module('nflowVisApp.layout', []);

  m.directive('layout', function() {
    return {
      replace: 'true',
      templateUrl: 'app/layout/layout.html'
    };
  });

  m.directive('pageHeader', function() {
    return {
      replace: 'true',
      templateUrl: 'app/layout/header.html',
      controller: 'PageHeaderCtrl as ctrl'
    };
  });

  m.directive('pageFooter', function() {
    return {
      replace: 'true',
      templateUrl: 'app/layout/footer.html'
    };
  });

  m.controller('PageHeaderCtrl', function($location) {
    var self = this;
    // nope, $stateParams.radiator wont work here
    self.radiator = !!$location.search().radiator;
  });

})();
