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

  m.controller('WorkflowManageSignalCtrl', function($state, WorkflowService, toastr) {
    var model = {};
    var self = this;
    self.model = model;
    self.signalWorkflow = signalWorkflow;

    function signalWorkflow() {
      var request = {
        signal: model.signal.value,
        reason: model.signalReason
      };
      WorkflowService.signal(self.workflow.id, request).then(updatedSuccess, updateFailed);
    }

    function updatedSuccess() {
      toastr.success('Workflow instance signal updated');
      $state.reload();
    }

    function updateFailed() {
      toastr.error('Workflow instance signal update failed');
    }
  });
})();
