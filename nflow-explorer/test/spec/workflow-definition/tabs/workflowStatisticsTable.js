'use strict';

describe('Controller: WorkflowStatisticsTable', function () {
  var $controller, $scope, $rootScope, WorkflowStatsPoller, WorkflowDefinitionGraphApi;

  beforeEach(module('nflowExplorer.workflowDefinition.tabs.workflowStatisticsTable'));
  beforeEach(module('nflowExplorer.services.WorkflowStatsPoller'));
  beforeEach(module('nflowExplorer.workflowDefinition.graph'));

  beforeEach(inject(function (_$controller_, _$rootScope_, _WorkflowDefinitionGraphApi_, _WorkflowStatsPoller_) {
    $controller = _$controller_;
    $rootScope = _$rootScope_;
    WorkflowStatsPoller = _WorkflowStatsPoller_;
    WorkflowDefinitionGraphApi = _WorkflowDefinitionGraphApi_;
    $scope = $rootScope.$new();
    $scope.definition = {
      type: 'dummy'
    };
  }));

  describe('Total instances calculation', function () {
    var ctrl;

    it('Workflow with two states', function () {
      stubStatePoller(WorkflowStatsPoller, {
        state1: createStatsForState(2, 1, 4, 3, 5, 6, 7),
        state2: createStatsForState(1, 1, 1, 1, 1, 1, 1)
      });
      ctrl = getCtrl(WorkflowStatsPoller, $scope);
      expect($scope.stats.stateStatisticsTotal).toEqual({
        allInstances: 29,
        created: {
          allInstances: 3,
          queuedInstances: 2
        },
        inProgress: {
          allInstances: 5,
          queuedInstances: 4
        },
        executing: {
          allInstances: 6
        },
        manual: {
          allInstances: 7
        },
        finished: {
          allInstances: 8
        }
      });
    });
  });

  function getCtrl(WorkflowStatsPoller, $scope) {
    return $controller('WorkflowStatisticsTable', {
      $scope: $scope,
      WorkflowStatsPoller: WorkflowStatsPoller,
      WorkflowDefinitionGraphApi: WorkflowDefinitionGraphApi
    });
  }

});
