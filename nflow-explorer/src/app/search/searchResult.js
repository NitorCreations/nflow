(function () {
  'use strict';

  var m = angular.module('nflowExplorer.search.searchResult', [
    'nflowExplorer.components',
    'nflowExplorer.config'
  ]);

  m.directive('searchResult', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        results: '=',
        definitions: '='
      },
      bindToController: true,
      controller: 'SearchResultCtrl as ctrl',
      templateUrl: 'app/search/searchResult.html'
    };
  });

  m.controller('SearchResultCtrl', function(WorkflowStateType, config) {
    var self = this;
    self.getStateClass = getStateClass;
    self.formatStateVariable = formatStateVariable;
    self.isStateVariableField = isStateVariableField;
    self.columns = config.searchResultColumns ? config.searchResultColumns : [
      {
        field: 'state',
        label: 'State'
      },
      {
        field: 'stateText',
        label: 'State text'
      },
      {
        field: 'status',
        label: 'Status'
      },
      {
        field: 'businessKey',
        label: 'Business key'
      },
      {
        field: 'externalId',
        label: 'External id'
      },
      {
        field: 'retries',
        label: 'Retries'
      },
      {
        field: 'created',
        label: 'Created',
        type: 'timestamp'
      },
      {
        field: 'started',
        label: 'Started',
        type: 'timestamp'
      },
      {
        field: 'modified',
        label: 'Modified',
        type: 'timestamp'
      },
      {
        field: 'nextActivation',
        label: 'Next activation',
        type: 'timestamp'
      },
    ];

    var classByStateType = {};
    classByStateType[WorkflowStateType.NORMAL] = 'info';
    classByStateType[WorkflowStateType.MANUAL] = 'warning';
    classByStateType[WorkflowStateType.END] = 'success';
    classByStateType[WorkflowStateType.ERROR] = 'danger';

    function getStateClass(result) {
      if (!result) { return ''; }

      var d = _.find(self.definitions, function (d) { return d.type === result.type; });
      if (d) {
        var state = _.find(d.states, function (s) { return s.id === result.state; });
        if (state) {
          if (state.id === d.onError) { return 'danger'; }
          return classByStateType[state.type] || '';
        }
      }
      return '';
    }

    function formatStateVariable(field, result) {
      if (!result.stateVariables) {
        return '';
      }
      return result.stateVariables[field.replace('stateVariables.','')];
    }

    function isStateVariableField(field) {
      return field.startsWith('stateVariables.');
    }
  });

})();
