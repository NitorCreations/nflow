(function () {
  'use strict';

  var m = angular.module('nflowExplorer.search.searchForm', [
    'nflowExplorer.search.criteriaModel',
    'nflowExplorer.services',
    'nflowExplorer.util'
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

  m.controller('SearchFormCtrl', function(CriteriaModel, WorkflowSearch, $timeout) {
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
      var t = $timeout(function() {
        self.showIndicator = true;
      }, 500);
      function hide() {
        $timeout.cancel(t);
        self.showIndicator = false;
      }
      self.results = WorkflowSearch.query(CriteriaModel.toQuery());
      self.results.$promise.then(hide, hide);
    }
  });

})();
