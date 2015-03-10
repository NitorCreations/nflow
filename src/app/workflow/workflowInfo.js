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
      var lastAction = _.last(_.result(self, 'workflow.actions'));
      return _.result(lastAction, 'executionEndTime', '');
    }
  });
})();
