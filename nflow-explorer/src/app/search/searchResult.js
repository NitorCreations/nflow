(function () {
  'use strict';

  var m = angular.module('nflowExplorer.search.searchResult', [
    'nflowExplorer.components',
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

  m.controller('SearchResultCtrl', function(WorkflowStateType) {
    var self = this;
    self.getStateClass = getStateClass;

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

  });

})();
