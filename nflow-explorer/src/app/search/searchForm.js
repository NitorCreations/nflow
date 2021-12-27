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

  m.controller('SearchFormCtrl', function($state, $timeout, CriteriaModel, WorkflowService, WorkflowInstanceStatus) {
    var self = this;
    self.showIndicator = false;
    self.instanceStatuses = _.values(WorkflowInstanceStatus);
    self.model = CriteriaModel.model;
    self.search = navigateSearch;
    self.executeSearch = executeSearch;
    self.onTypeChange = CriteriaModel.onDefinitionChange;
    self.wildCardTooltip = 'Use % to replace many characters and _ to replace a single character';

    initialize();

    function initialize() {
      if (!CriteriaModel.isEmpty()) {
        executeSearch();
      }
    }

    function navigateSearch() {
      $state.go('search', {
        type: (self.model.definition && self.model.definition.type) || 'all',
        state: self.model.state && self.model.state.id,
        status: self.model.status,
        businessKey: self.model.businessKey,
        externalId: self.model.externalId,
        id: self.model.id,
        parentWorkflowId: self.model.parentWorkflowId,
      }, {
        reload: true
      });
    }

    function executeSearch() {
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
