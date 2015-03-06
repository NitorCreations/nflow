(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflow.tabs.actionHistory', [
    'nflowVisApp.workflow.graph'
  ]);

  m.directive('workflowTabActionHistory', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        workflow: '='
      },
      bindToController: true,
      controller: 'WorkflowTabActionHistoryCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/tabs/actionHistory.html'
    };
  });

  m.controller('WorkflowTabActionHistoryCtrl', function(WorkflowGraphApi) {
    var self = this;

    self.selectAction = WorkflowGraphApi.onSelectNode;
    self.duration = duration;

    function duration(action) {
      var start = moment(action.executionStartTime);
      var end = moment(action.executionEndTime);
      if(!start || !end) {
        return '-';
      }
      var d = moment.duration(end.diff(start));
      if(d < 1000) {
        return d + ' msec';
      }
      return d.humanize();
    }
  });
})();

