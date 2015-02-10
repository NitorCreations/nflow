(function () {
  'use strict';

  var m = angular.module('nflowVisApp.search', []);

  m.controller('WorkflowSearchCtrl', function ($scope, $routeParams, WorkflowDefinitions, WorkflowSearch) {
    $scope.results = [];
    $scope.crit = {};

    $scope.search = function search() {
      var query = {};
      for (var i in $scope.crit) {
        query[i] = $scope.crit[i];
      }

      if (query.type) {
        query.type = query.type.type;
      }
      if (query.state) {
        query.state = query.state.name;
      }

      // set state to undef if it is not found in selected definition
      if (query.type && query.state) {
        var stateInDefinition = _.first(_.filter($scope.crit.type.states, function (state) {
          return state.name === query.state;
        }));
        if (!stateInDefinition) {
          query.state = undefined;
        }
      }

      query = _.omit(query, function (value) {
        return (value === undefined || value === null);
      });
      $scope.results = WorkflowSearch.query(query);
    };

    function handleParams() {
      var paramType = _.first(_.filter($scope.definitions, function (def) {
        return def.type === $routeParams.type;
      }));
      if (paramType) {
        $scope.crit.type = paramType;
        var paramState = _.first(_.filter(paramType.states, function (state) {
          return state.name === $routeParams.state;
        }));
        $scope.crit.state = paramState;
      }
      if (Object.keys($scope.crit).length > 0) {
        $scope.search();
      }
    }

    WorkflowDefinitions.query(function (definitions) {
      $scope.definitions = definitions;
      handleParams();
    });

    $scope.hasResults = function hasResults() {
      return !!_.first($scope.results);
    };

    function getDefinition(type) {
      return _.find($scope.definitions, function (def) {
        return def.type === type;
      });
    }

    $scope.getStateClass = function getStateClass(result) {
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
    };
  });
})();
