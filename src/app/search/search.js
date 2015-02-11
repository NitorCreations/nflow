(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search', [
    'nflowVisApp.search.criteriaModel',
    'nflowVisApp.search.searchForm',
    'nflowVisApp.search.searchResult'
  ]);

  m.controller('WorkflowSearchCtrl', function ($routeParams, definitions, CriteriaModel) {
    var self = this;
    self.definitions = definitions;
    self.results = [];
    self.hasResults = hasResults;

    CriteriaModel.initialize({ type: $routeParams.type, stateName: $routeParams.state }, definitions);

    function hasResults() {
      return !_.isEmpty(self.results);
    }
  });

})();
