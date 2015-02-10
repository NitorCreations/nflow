(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search', [
    'nflowVisApp.search.searchResult'
  ]);

  m.controller('WorkflowSearchCtrl', function ($routeParams, WorkflowDefinitions, WorkflowSearch) {
    var self = this;
    self.results = [];
    self.hasResults = hasResults;
    self.crit = {};
    self.search = search;
    self.definitions = [];

    initialize();

    function initialize() {
      WorkflowDefinitions.query(function (definitions) {
        self.definitions = definitions;
        handleParams();
      });
    }

    function search() {
      var query = {};
      for (var i in self.crit) {
        query[i] = self.crit[i];
      }

      if (query.type) {
        query.type = query.type.type;
      }
      if (query.state) {
        query.state = query.state.name;
      }

      // set state to undef if it is not found in selected definition
      if (query.type && query.state) {
        var stateInDefinition = _.first(_.filter(self.crit.type.states, function (state) {
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

    function handleParams() {
      var paramType = _.first(_.filter(self.definitions, function (def) {
        return def.type === $routeParams.type;
      }));
      if (paramType) {
        self.crit.type = paramType;
        var paramState = _.first(_.filter(paramType.states, function (state) {
          return state.name === $routeParams.state;
        }));
        self.crit.state = paramState;
      }
      if (Object.keys(self.crit).length > 0) {
        search();
      }
    }

    function hasResults() {
      return !!_.first(self.results);
    }

  });
})();
