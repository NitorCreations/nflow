'use strict';

function stubStatePoller(WorkflowStatsPoller, stats) {
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
