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

  m.controller('PageHeaderCtrl', function($location, $state, $window) {
    var self = this;
    // nope, $stateParams.radiator wont work here
    self.radiator = !!$location.search().radiator;
    self.isFrontPageTabActive = function() { return $state.includes('frontPageTab'); };
    self.isExecutorsTabActive = function() { return $state.includes('executorsTab'); };
    self.isSearchTabActive = function() { return $state.includes('searchTab'); };
    self.isAboutTabActive = function() { return $state.includes('aboutTab'); };
    self.returnUrl = getServerParamFromUrl('returnUrl', $window);
    self.returnUrlLabel = getServerParamFromUrl('returnUrlLabel', $window) || 'Back';

    function getServerParamFromUrl(paramName, $window) {
      var searchStr = $window.location.search.substring(1);
      var keyValues = searchStr.split('&');
      for (var i=0; i<keyValues.length; i++) {
        var split = keyValues[i].split('=');
        if (split[0] === paramName) {
          return decodeURIComponent(split[1]);
        }
      }
    }
  });

})();
