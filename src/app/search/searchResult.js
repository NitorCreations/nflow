(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search.searchResult', [
    'nflowVisApp.filters'
  ]);

  m.directive('searchResult', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        results: '=',
        definitions: '='
      },
      bindToController: true,
      controller: 'SearchResultCtrl as ctrl',
      templateUrl: 'app/search/searchResult.html'
    };
  });

  m.controller('SearchResultCtrl', function() {
    var self = this;
    self.getStateClass = getStateClass;

    function getStateClass(result) {
      if (!result) {
        return '';
      }
      var def = getDefinition(result.type);
      if (!def) {
        return '';
      }
      var state = _.find(def.states, function (s) {
        return s.id === result.state;
      });
      if (state.id === def.onError) {
        return 'danger';
      }
      if (state.type === 'normal') {
        return 'info';
      } else if (state.type === 'manual') {
        return 'warning';
      } else if (state.type === 'end') {
        return 'success';
      } else if (state.type === 'error') {
        return 'danger';
      }
      return '';
    }


    function getDefinition(type) {
      return _.find(self.definitions, function (def) {
        return def.type === type;
      });
    }

  });

})();
