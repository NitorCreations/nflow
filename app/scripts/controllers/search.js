'use strict';

angular.module('nflowVisApp')
.controller('WorkflowSearchCtrl', function($scope, $routeParams, WorkflowDefinitions, WorkflowSearch) {
  $scope.results = [];
  $scope.crit = {};

  $scope.search = function search() {
    var query = {};
    for (var i in $scope.crit) {
        query[i] = $scope.crit[i];
    }
    if(query.type && typeof(query.type) !== 'string' ) {
      query.type = query.type.type;
    }
    query = _.omit(query, function(value) {
      return (value === undefined || value === null);
    });
    $scope.results = WorkflowSearch.query(query);
  };

  function handleParams() {
    var paramType = _.first(_.filter($scope.definitions, function(def) {
      return def.type === $routeParams.type;
    }));
    if(paramType) {
      $scope.crit.type = paramType;
    }
    $scope.crit.state = $routeParams.state;
    if(Object.keys($scope.crit).length > 0) {
      $scope.search();
    }
  }

  WorkflowDefinitions.query(function(definitions) {
    $scope.definitions = definitions;
    handleParams();
  });

  $scope.hasResults = function hasResults() {
    return !!_.first($scope.results);
  };

  function getDefinition(type) {
    return _.find($scope.definitions, function(def) {
      return def.type === type;
    });
  }
  $scope.getStateClass = function getStateClass(result) {
    if(!result) {
      return '';
    }
    var def = getDefinition(result.type);
    if(!def) {
      return '';
    }
    var state = _.find(def.states, function(s) {
      return s.id === result.state;
    });
    if(state.id === def.onError) {
      return 'danger';
    }
    if(state.type === 'normal') {
      return 'info';
    } else if (state.type === 'manual') {
      return 'warning';
    } else if(state.type === 'end') {
      return 'success';
    } else if(state.type === 'error') {
      return 'danger';
    }
    return '';
  };

});
