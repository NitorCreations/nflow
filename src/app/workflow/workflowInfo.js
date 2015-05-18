(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow.info', [
    'nflowExplorer.workflow.graph'
  ]);

  m.directive('workflowInfo', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        workflow: '=',
        parentWorkflow: '=',
        childWorkflows: '=',
      },
      bindToController: true,
      controller: 'WorkflowInfoCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/workflowInfo.html'
    };
  });

  m.controller('WorkflowInfoCtrl', function(WorkflowGraphApi) {
    var self = this;
    self.currentStateTime = currentStateTime;
    self.selectAction = WorkflowGraphApi.onSelectNode;

    function currentStateTime() {
      var lastAction = _.last(_.result(self, 'workflow.actions'));
      return _.result(lastAction, 'executionEndTime', '');
    }
  });
})();
