(function () {
  'use strict';

var m = angular.module('nflowVisApp.workflow', [
  'nflowVisApp.workflow.graph'
]);
  m.controller('WorkflowCtrl', function WorkflowCtrl(Workflows, ManageWorkflow, $state, $rootScope, workflow, definition, WorkflowGraphApi) {
    var self = this;

    self.workflow = workflow;
    self.definition = definition;

    self.manage = {};
    self.manage.timeUnits = ['minutes','hours','days'];
    self.manage.timeUnit = self.manage.timeUnits[0];
    self.manage.duration = 0;

    self.getClass = getClass;
    self.selectAction = selectAction;
    self.duration = duration;
    self.currentStateTime= currentStateTime;
    self.updateWorkflow = updateWorkflow;
    self.stopWorkflow = stopWorkflow;
    self.pauseWorkflow = pauseWorkflow;
    self.resumeWorkflow = resumeWorkflow;

    initialize();

    function initialize() {
      defaultNextState(self.workflow.state);


      WorkflowGraphApi.registerOnSelectNodeListener(function(nodeId) {
        defaultNextState(nodeId);
      });
    }

    function defaultNextState(stateName) {
      self.manage.nextState = _.first(_.filter(self.definition.states, function(state) {
        return state.name === stateName;
      }));
    }

    function getClass(action) {
      // See http://getbootstrap.com/css/#tables
      if(!action.type) {
        return '';
      }
      return {'stateExecution' : 'success',
              'stateExecutionFailed' :'danger',
              'externalChange' : 'info',
              'recovery': 'warning'}[action.type];
    }

    function selectAction(action) {
      var state = action;
      if(typeof(action) !== 'string') {
        state = action.state;
      }
      console.log('Action selected', state);
      WorkflowGraphApi.onSelectNode(state);
    }

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

    function currentStateTime() {
      if(!self.workflow) {
        return '';
      }
      var lastAction = _.last(self.workflow.actions);
      if(!lastAction) {
        return '';
      }
      return lastAction.executionEndTime;
    }

    function updateWorkflow(manage) {
      console.info('updateWorkflow()', manage);
      var now = moment(new Date());
      var request = {};
      if(manage.nextState) {
        request.state = manage.nextState.name;
      }
      if((manage.duration !== undefined && manage.duration !== null) && manage.timeUnit) {
        request.nextActivationTime = now.add(moment.duration(manage.duration, manage.timeUnit));
      }
      if(manage.actionDescription) {
        request.actionDescription = manage.actionDescription;
      }

      Workflows.update({id: workflow.id}, request, refresh);
    }

    function stopWorkflow(manage) {
      console.info('stopWorkflow()', manage);
      ManageWorkflow.stop(workflow.id, manage.actionDescription).then(refresh);
    }

    function pauseWorkflow(manage) {
      console.info('pauseWorkflow()', manage);
      ManageWorkflow.pause(workflow.id, manage.actionDescription).then(refresh);
    }

    function resumeWorkflow(manage) {
      console.info('resumeWorkflow()', manage);
      ManageWorkflow.resume(workflow.id, manage.actionDescription).then(refresh);
    }

    function refresh() {
      $state.reload();
    }
  });
})();
