(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search.searchForm', [
    'nflowVisApp.search.criteriaModel',
    'nflowVisApp.services',
    'nflowVisApp.util'
  ]);

  m.directive('searchForm', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        results: '=',
        definitions: '='
      },
      bindToController: true,
      controller: 'SearchFormCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/search/searchForm.html'
    };
  });

  m.controller('SearchFormCtrl', function(CriteriaModel, WorkflowSearch) {
    var self = this;
    self.model = CriteriaModel.model;
    self.search = search;
    self.onTypeChange = CriteriaModel.onDefinitionChange;

    initialize();

    function initialize() {
      if (!CriteriaModel.isEmpty()) {
        search();
      }
    }

    function search() {
      self.results = WorkflowSearch.query(CriteriaModel.toQuery());
    }
  });

})();
