(function () {
  'use strict';

  var m = angular.module('nflowExplorer.layout', []);

  m.directive('layout', function() {
    return {
      restrict: 'E',
      replace: 'true',
      templateUrl: 'app/layout/layout.html'
    };
  });

  m.directive('pageHeader', function() {
    return {
      restrict: 'E',
      replace: 'true',
      templateUrl: 'app/layout/header.html',
      controller: 'PageHeaderCtrl as ctrl'
    };
  });

  m.directive('pageFooter', function() {
    return {
      restrict: 'E',
      replace: 'true',
      templateUrl: 'app/layout/footer.html'
    };
  });

  m.controller('PageHeaderCtrl', function($location, $state) {
    var self = this;
    // nope, $stateParams.radiator wont work here
    self.radiator = !!$location.search().radiator;
    self.isFrontPageTabActive = function() { return $state.includes('frontPageTab'); };
    self.isSearchTabActive = function() { return $state.includes('searchTab'); };
    self.isAboutTabActive = function() { return $state.includes('aboutTab'); };
  });

})();
