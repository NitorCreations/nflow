(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search.searchForm', [
    'nflowVisApp.services',
    'nflowVisApp.util'
  ]);

  m.directive('searchForm', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        results: '=',
        definitions: '=',
        criteria: '='
      },
      bindToController: true,
      controller: 'SearchFormCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/search/searchForm.html'
    };
  });

  m.controller('SearchFormCtrl', function(SearchFormService) {
    var self = this;
    self.search = search;
    self.results = SearchFormService.results;

    initialize();

    function initialize() {
      if (_.keys(self.criteria).length > 0) {
        search();
      }
    }

    function search() {
      return SearchFormService.search(self.criteria);
    }

  });

  m.factory('SearchFormService', function(WorkflowSearch){
    var api = {};
    api.results = [];
    api.search = search;
    return api;

    function search(criteria) {
      var query = {};
      for (var i in criteria) {
        query[i] = criteria[i];
      }

      if (query.type) {
        query.type = query.type.type;
      }
      if (query.state) {
        query.state = query.state.name;
      }

      // set state to undef if it is not found in selected definition
      if (query.type && query.state) {
        var stateInDefinition = _.first(_.filter(criteria.type.states, function (state) {
          return state.name === query.state;
        }));
        if (!stateInDefinition) {
          query.state = undefined;
        }
      }

      query = _.omit(query, function (value) {
        return (value === undefined || value === null);
      });

      WorkflowSearch.query(query, function(results) {
        angular.copy(results, api.results);
      });
    }

  });

})();
