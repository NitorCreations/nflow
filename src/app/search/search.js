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
      var paramType = _.first(_.filter(self.definitions, function (def) {
        return def.type === $routeParams.type;
      }));
      if (paramType) {
        self.criteria.type = paramType;
        var paramState = _.first(_.filter(paramType.states, function (state) {
          return state.name === $routeParams.state;
        }));
        self.criteria.state = paramState;
      }
    }

    function hasResults() {
      return !!_.first(self.results);
    }

  });

})();
