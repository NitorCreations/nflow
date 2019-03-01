(function () {
  'use strict';

  var m = angular.module('nflowExplorer.components.customContent', []);

  m.directive('customContent', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        func: '&',
      },
      bindToController: true,
      controller: 'CustomContentCtrl as ctrl',
      templateUrl: 'app/components/customContent.html'
    };
  });

  m.controller('CustomContentCtrl', function CustomContentCtrl() {
    var self = this;

    self.$onInit = function() {
      if (self.func && typeof(self.func) === 'function') {
        try {
          var result = self.func();
          if (!result) {
            self.content = '';
            return;
          }
          if (typeof(result) === 'string') {
            self.content = result;
            return;
          }
          result.then(function(value) {
            self.content = value;
          }).catch(function(err) {
            console.error('customContent function returned rejecting Promise', err);
            self.content = '';
          });
        } catch(e) {
          console.error('customContent function threw exception', e);
          self.content = '';
        }
      }
    };
  });

})();

