(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow', [
    'nflowExplorer.workflow.graph',
    'nflowExplorer.workflow.info',
    'nflowExplorer.workflow.tabs',
    'nflowExplorer.services.WorkflowService'
  ]);

  m.controller('WorkflowCtrl', function (workflow, definition,
      parentWorkflow, childWorkflows, $scope, config, $interval, WorkflowService) {
    var self = this;
    self.workflow = workflow;
    self.parentWorkflow = parentWorkflow;
    self.definition = definition;
    self.childWorkflows = childWorkflows;

    function reloadWorkflow(workflowId) {
      console.log('Fetching workflow id ' + workflowId);
      WorkflowService.get(workflowId).then(function(workflow) {
        self.workflow = workflow;
      });
    }
    self.poller = $interval(function() { reloadWorkflow(self.workflow.id); },
      config.radiator.pollPeriod * 1000);
    $scope.$on('$destroy', function() {
      console.log('Stop polling workflow id ' + self.workflow.id);
      $interval.cancel(self.poller);
    });
  });
})();
