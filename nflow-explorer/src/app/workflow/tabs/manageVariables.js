(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow.tabs.manageVariables', [
    'nflowExplorer.services',
    'ui.router'
  ]);

  m.directive('workflowTabManageVariables', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '=',
        workflow: '='
      },
      bindToController: true,
      controller: 'WorkflowManageVariablesCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/tabs/manageVariables.html'
    };
  });

  m.controller('WorkflowManageVariablesCtrl', function($state, WorkflowService, toastr) {
    var model = {};
    var self = this;
    self.model = model;
    self.updateWorkflow = updateWorkflow;

    function updateWorkflow() {
      console.info('updateWorkflow()', model);
      var request = {};
      if (model.variableName && model.variableValue) {
        request.stateVariables = {};
        request.stateVariables[model.variableName] = model.variableValue;
      }
      if (model.actionDescription) {
        request.actionDescription = model.actionDescription;
      }
      WorkflowService.update(self.workflow.id, request).then(updateSuccess, updateFailed);
    }

    function updateSuccess() {
      toastr.success('Workflow instance variables updated');
      $state.reload();
    }

    function updateFailed() {
      toastr.error('Workflow instance variables update failed');
    }
  });
})();
