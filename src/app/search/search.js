(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search', []);

  m.controller('WorkflowSearchCtrl', function ($routeParams, WorkflowDefinitions, WorkflowSearch) {
    var self = this;
    self.results = [];
    self.hasResults = hasResults;
    self.crit = {};
    self.search = search;
    self.getStateClass = getStateClass;
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

    function getDefinition(type) {
      return _.find(self.definitions, function (def) {
        return def.type === type;
      });
    }

    function getStateClass(result) {
      if (!result) {
        return '';
      }
      var def = getDefinition(result.type);
      if (!def) {
        return '';
      }
      var state = _.find(def.states, function (s) {
        return s.id === result.state;
      });
      if (state.id === def.onError) {
        return 'danger';
      }
      if (state.type === 'normal') {
        return 'info';
      } else if (state.type === 'manual') {
        return 'warning';
      } else if (state.type === 'end') {
        return 'success';
      } else if (state.type === 'error') {
        return 'danger';
      }
      return '';
    }
  });
})();
