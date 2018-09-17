(function () {
  'use strict';

  var m = angular.module('nflowExplorer.components.util', []);

  m.directive('emptyToNull', function() {
    return {
      restrict: 'A',
      require: 'ngModel',
      link: function (scope, elem, attrs, ctrl) {
        ctrl.$parsers.push(function(viewValue) {
          if(viewValue === '') {
            return null;
          }
          return viewValue;
        });
      }
    };
  });

})();
