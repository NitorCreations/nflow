(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow.tabs.manage', [
    'nflowExplorer.workflow.graph',
    'nflowExplorer.services',
    'ui.router'
  ]);

  m.directive('workflowTabManage', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        definition: '=',
        workflow: '='
      },
      bindToController: true,
      controller: 'WorkflowManageCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/tabs/manage.html'
    };
  });

  m.controller('WorkflowManageCtrl', function($state, Workflows, ManageWorkflow, WorkflowGraphApi) {
    var model = {};
    model.timeUnits = ['minutes', 'hours', 'days'];
    model.timeUnit = model.timeUnits[0];
    model.duration = 0;

    var self = this;
    self.model = model;

    self.updateWorkflow = updateWorkflow;
    self.stopWorkflow = stopWorkflow;
    self.pauseWorkflow = pauseWorkflow;
    self.resumeWorkflow = resumeWorkflow;

    self.isPauseDisabled = isPauseDisabled;
    self.isResumeDisabled = isResumeDisabled;
    self.isStopDisabled = isStopDisabled;

    initialize();

    function initialize() {
      defaultNextState(self.workflow.state);

      WorkflowGraphApi.registerOnSelectNodeListener(function(nodeId) {
        defaultNextState(nodeId);
      });
    }

    function defaultNextState(stateName) {
      model.nextState = _.first(_.filter(self.definition.states, function(state) {
        return state.name === stateName;
      }));
    }

    function updateWorkflow() {
      console.info('updateWorkflow()', model);
      var now = moment(new Date());
      var request = {};
      if(model.nextState) {
        request.state = model.nextState.name;
      }
      if(_.isNumber(model.duration) && model.timeUnit) {
        request.nextActivationTime = now.add(moment.duration(model.duration, model.timeUnit));
      }
      if(model.actionDescription) {
        request.actionDescription = model.actionDescription;
      }

      Workflows.update({id: self.workflow.id}, request, refresh);
    }

    function stopWorkflow() {
      console.info('stopWorkflow()', model);
      ManageWorkflow.stop(self.workflow.id, model.actionDescription).then(refresh);
    }

    function pauseWorkflow() {
      console.info('pauseWorkflow()', model);
      ManageWorkflow.pause(self.workflow.id, model.actionDescription).then(refresh);
    }

    function resumeWorkflow() {
      console.info('resumeWorkflow()', model);
      ManageWorkflow.resume(self.workflow.id, model.actionDescription).then(refresh);
    }

    function isPauseDisabled() { return _.isUndefined(self.workflow.nextActivation) || isPaused(); }

    function isResumeDisabled() { return !isPaused(); }

    function isStopDisabled() { return _.isUndefined(self.workflow.nextActivation); }

    function isPaused() { return self.workflow.status === 'paused'; }

    function refresh() { $state.reload(); }
  });
})();
