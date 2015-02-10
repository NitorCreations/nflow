(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search', [
    'nflowVisApp.search.searchForm',
    'nflowVisApp.search.searchResult'
  ]);

  m.controller('WorkflowSearchCtrl', function ($routeParams, definitions) {
    var self = this;
    self.definitions = definitions;
    self.results = [];
    self.criteria = {};
    self.hasResults = hasResults;

    initialize();

    function initialize() {
      var type = self.criteria.type = definitionTypeFromRoute(self.definitions);
      if (type) {
        self.criteria.state = typeStateFromRoute(type);
      }

      function definitionTypeFromRoute(definitions) {
        return _.find(definitions, function (d) { return d.type === $routeParams.type; });
      }

      function typeStateFromRoute(type) {
          return _.find(type.states, function (s) { return s.name === $routeParams.state; });
      }
    }

    function hasResults() {
      return !!_.first(self.results);
    }

  });

})();
