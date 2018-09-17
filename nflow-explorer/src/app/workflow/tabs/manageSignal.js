(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow.tabs.manageSignal', [
    'nflowExplorer.services',
    'ui.router'
  ]);

  m.directive('workflowTabManageSignal', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '=',
        workflow: '='
      },
      bindToController: true,
      controller: 'WorkflowManageSignalCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/tabs/manageSignal.html'
    };
  });

  m.controller('WorkflowManageSignalCtrl', function($state, WorkflowService) {
    var model = {};
    var self = this;
    self.model = model;
    self.signalWorkflow = signalWorkflow;

    function signalWorkflow() {
      var request = {
        signal: model.signal.value,
        reason: model.signalReason
      };
      WorkflowService.signal(self.workflow.id, request).then(refresh);
    }

    function refresh() { $state.reload(); }
  });
})();
