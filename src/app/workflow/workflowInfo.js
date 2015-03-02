(function () {
  'use strict';

  var m = angular.module('nflowVisApp.workflow.info', [
    'nflowVisApp.workflow.graph'
  ]);

  m.directive('workflowInfo', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        workflow: '='
      },
      bindToController: true,
      controller: 'WorkflowInfoCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/workflowInfo.html'
    };
  });

  m.controller('WorkflowInfoCtrl', function(WorkflowGraphApi) {
    var self = this;

    self.currentStateTime= currentStateTime;
    self.selectAction = WorkflowGraphApi.onSelectNode;

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
  });
})();
