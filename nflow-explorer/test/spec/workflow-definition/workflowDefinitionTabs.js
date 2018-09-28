'use strict';

describe('Controller: WorkflowDefinitionTabsCtrl', function () {
  var $controller, $scope, $rootScope, WorkflowStatsPoller, definitionStateStats;

  beforeEach(module('nflowExplorer.workflowDefinition.tabs'));
  beforeEach(module('nflowExplorer.services.WorkflowStatsPoller'));

  beforeEach(inject(function (_$controller_, _$rootScope_, _WorkflowStatsPoller_) {
    $controller = _$controller_;
    $rootScope = _$rootScope_;
    WorkflowStatsPoller = _WorkflowStatsPoller_;
    $scope = $rootScope.$new();
    $scope.definition = {
      type: 'dummy'
    };
  }));

  describe('Bar chart data checks', function () {
    var ctrl;

    it('Stats with manual states only are filtered', function () {
      stubStatePoller({
        done: createStatsForState(0,0,0,0,0,0,1)
      });
      ctrl = getCtrl(WorkflowStatsPoller, $scope);
      expect($scope.labels.length).toEqual(0);
      expect($scope.data[0].length).toEqual(0);
    });

    it('Instances are summed correctly for metastates', function () {
      stubStatePoller({
        state1: createStatsForState(2,1,4,3,5,6,7)
      });
      ctrl = getCtrl(WorkflowStatsPoller, $scope);
      expect($scope.labels).toEqual(['state1']);
      expect($scope.data).toEqual([[2],[4],[5],[6]]);
    });

    it('All non-end definition states are shown regardless of stats', function () {
      stubStatePoller({
        state1: createStatsForState(1,0,0,0,0,0,0)
      });
      $scope.definition = {
        type: 'dummy',
        states: [{
          id: 'state2',
          type: 'normal'
        }, {
          id: 'state3',
          type: 'end'
        }]
      };
      ctrl = getCtrl(WorkflowStatsPoller, $scope);
      expect($scope.labels).toEqual(['state1','state2']);
      expect($scope.data).toEqual([[1,0],[0,0],[0,0],[0,0]]);
    });
  });

  function getCtrl(WorkflowStatsPoller, $scope) {
    return $controller('WorkflowDefinitionTabsCtrl', {
      $scope: $scope,
      WorkflowStatsPoller: WorkflowStatsPoller
    });
  }

  function stubStatePoller(stats) {
    sinon.stub(WorkflowStatsPoller, 'getLatest').callsFake(function() {
      return {
        stateStatistics: stats
      };
    });
    sinon.stub(WorkflowStatsPoller, 'start').callsFake(function() {});
  }

  function createStatsForState(cai, cqi, ipai, ipqi, eai, mai, fai) {
    return {
      created: {
        allInstances: cai,
        queuedInstances: cqi
      },
      inProgress: {
        allInstances: ipai,
        queuedInstances: ipqi
      },
      executing: {
        allInstances: eai
      },
      manual: {
        allInstances: mai
      },
      finished: {
        allInstances: fai
      }
    };
  }

});
