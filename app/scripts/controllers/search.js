'use strict';

var app = angular.module('nflowVisApp');

app.factory('WorkflowSearch', function ($resource, config) {
  return $resource(config.nflowUrl + '/v1/workflow-instance',
                   {include: 'actions,currentStateVariables,actionStateVariables'}
                  );
});

app.controller('WorkflowSearchCtrl', function($scope, $routeParams, WorkflowDefinitions, WorkflowSearch) {
  $scope.results = [];
  $scope.crit = {};
  $scope.definitions = WorkflowDefinitions.query();
  $scope.search = function search() {

    var query = {};
    for (var i in $scope.crit) {
        query[i] = $scope.crit[i];
    }
    if(query.type && typeof(query.type) !== 'string' ) {
      query.type = query.type.type;
    }
    console.log('search:', query);
    $scope.results = WorkflowSearch.query(query);
  };

  if($routeParams.type) {
    $scope.crit.type = $routeParams.type;
    $scope.search();
  }

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
