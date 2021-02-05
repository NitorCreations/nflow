(function () {
  'use strict';

  var m = angular.module('nflowExplorer.workflow.info', [
    'nflowExplorer.components',
    'nflowExplorer.workflow.graph',
  ]);

  m.directive('workflowInfo', function() {
    return {
      restrict: 'E',
      replace: true,
      scope: {
        workflow: '=',
        parentWorkflow: '=',
        childWorkflows: '=',
        definition: '=',
      },
      bindToController: true,
      controller: 'WorkflowInfoCtrl',
      controllerAs: 'ctrl',
      templateUrl: 'app/workflow/workflowInfo.html'
    };
  });

  m.controller('WorkflowInfoCtrl', function(WorkflowGraphApi, config) {
    var self = this;
    self.currentStateTime = currentStateTime;
    self.selectAction = WorkflowGraphApi.onSelectNode;
    self.workflowInfoTable = config.workflowInfoTable;
    self.contentGenerator = config.customInstanceContent;

    function currentStateTime() {
      return _.result(self, 'modified', '');
    }
  });
})();
