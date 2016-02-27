(function () {
  'use strict';

  var m = angular.module('nflowExplorer.search.searchForm', [
    'nflowExplorer.search.criteriaModel',
    'nflowExplorer.services',
    'nflowExplorer.components',
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

  m.controller('SearchFormCtrl', function($timeout, CriteriaModel, WorkflowService, WorkflowInstanceStatus) {
    var self = this;
    self.showIndicator = false;
    self.instanceStatuses = _.values(WorkflowInstanceStatus);
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
      var t = $timeout(function() { self.showIndicator = true; }, 500);

      WorkflowService.query(CriteriaModel.toQuery())
        .then(function(results) {
          self.results = results;
          hideIndicator();
        }).catch(hideIndicator);

      function hideIndicator() {
        $timeout.cancel(t);
        self.showIndicator = false;
      }
    }
  });

})();
