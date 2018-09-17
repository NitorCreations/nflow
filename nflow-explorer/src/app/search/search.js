(function () {
  'use strict';

  var m = angular.module('nflowExplorer.search', [
    'nflowExplorer.search.criteriaModel',
    'nflowExplorer.search.searchForm',
    'nflowExplorer.search.searchResult'
  ]);

  m.controller('SearchCtrl', function ($stateParams, definitions, CriteriaModel) {
    var self = this;
    self.definitions = definitions;
    self.results = undefined;
    self.hasResults = hasResults;

    CriteriaModel.initialize({
        type: $stateParams.type,
        stateId: $stateParams.state,
        parentWorkflowId: toInt($stateParams.parentWorkflowId)
      },
      definitions);

    function hasResults() {
      return self.results !== undefined;
    }

    function toInt(value) {
      try {
        return parseInt(value);
      } catch(e) {
        return undefined;
      }
    }
  });

})();
