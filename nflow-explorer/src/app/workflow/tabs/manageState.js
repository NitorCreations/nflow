(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow.tabs.manageState', [
    'nflowExplorer.workflow.graph',
    'nflowExplorer.services',
    'ui.router'
  ]);

  m.directive('workflowTabManageState', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '=',
        workflow: '='
      },
      controller: 'WorkflowManageStateCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/tabs/manageState.html'
    };
  });

  m.controller('WorkflowManageStateCtrl', function($state, WorkflowService, WorkflowGraphApi, $scope, toastr) {
    var model = {};
    model.timeUnits = ['minutes', 'hours', 'days'];
    model.timeUnit = model.timeUnits[0];
    model.duration = 0;

    var self = this;
    self.model = model;
    self.definition = $scope.definition;
    self.workflow = $scope.workflow;
    self.updateWorkflow = updateWorkflow;

    $scope.safeApply = function(fn) {
      var phase = this.$root.$$phase;
      if(phase === '$apply' || phase === '$digest') {
        if(fn && (typeof(fn) === 'function')) {
          fn();
        }
      } else {
        this.$apply(fn);
      }
    };

    initialize();

    function initialize() {
      defaultNextState(self.workflow.state);
      WorkflowGraphApi.registerOnSelectNodeListener(function(nodeId) {
        // Triggered by angular event (e.g. select row from action history) or non angular event (click node directly).
        // Without $apply non angular events don't trigger digest and the state box is updated lazily.
        $scope.safeApply(function() {
          defaultNextState(nodeId);
        });
      });
    }

    function defaultNextState(stateId) {
      model.nextState = _.first(_.filter(self.definition.states, function(state) {
        return state.id === stateId;
      }));
    }

    function updateWorkflow() {
      console.info('updateWorkflow()', model);
      var now = moment(new Date());
      var request = {};
      if(model.nextState) {
        request.state = model.nextState.id;
      }
      if(_.isNumber(model.duration) && model.timeUnit) {
        request.nextActivationTime = now.add(moment.duration(model.duration, model.timeUnit));
      }
      if(model.actionDescription) {
        request.actionDescription = model.actionDescription;
      }

      WorkflowService.update(self.workflow.id, request).then(updatedSuccess, updateFailed);
    }

    function updatedSuccess() {
      toastr.success('Workflow instance state updated');
      $state.reload();
    }

    function updateFailed() {
      toastr.error('Workflow instance state update failed');
    }
  });
})();
