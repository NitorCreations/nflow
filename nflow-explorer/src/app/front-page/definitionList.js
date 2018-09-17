(function () {
  'use strict';

  var m = angular.module('nflowExplorer.frontPage.definitionList', []);

  m.directive('definitionList', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definitions: '='
      },
      bindToController: true,
      controller: 'DefinitionListCtrl as ctrl',
      templateUrl: 'app/front-page/definitionList.html'
    };
  });

  m.controller('DefinitionListCtrl', function($scope, $location) {
    $scope.showDefinition = function(type) {
      $location.path('workflow-definition/' + type);
    };
  });

})();
