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

  m.controller('WorkflowManageCtrl', function($state, Workflows, WorkflowGraphApi) {
    var model = {};
    model.timeUnits = ['minutes', 'hours', 'days'];
    model.timeUnit = model.timeUnits[0];
    model.duration = 0;

    var self = this;
    self.model = model;

    self.updateWorkflow = updateWorkflow;

    initialize();

    function initialize() {
      defaultNextState(self.workflow.state);

      WorkflowGraphApi.registerOnSelectNodeListener(function(nodeId) {
        defaultNextState(nodeId);
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

    function refresh() { $state.reload(); }
  });
})();
