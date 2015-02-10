(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search.searchForm', [
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

  m.controller('SearchFormCtrl', function(WorkflowSearch) {
    var self = this;
    self.search = search;

    initialize();

    function initialize() {
      if (_.keys(self.criteria).length > 0) {
        search();
      }
    }

    function search() {
      var query = {};
      for (var i in self.criteria) {
        query[i] = self.criteria[i];
      }

      if (query.type) {
        query.type = query.type.type;
      }
      if (query.state) {
        query.state = query.state.name;
      }

      // set state to undef if it is not found in selected definition
      if (query.type && query.state) {
        var stateInDefinition = _.first(_.filter(self.criteria.type.states, function (state) {
          return state.name === query.state;
        }));
        if (!stateInDefinition) {
          query.state = undefined;
        }
      }

      query = _.omit(query, function (value) {
        return (value === undefined || value === null);
      });
      self.results = WorkflowSearch.query(query);
    }

  });

})();
