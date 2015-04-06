'use strict';

var fixtures = {};

fixtures.nbankNflow = {
  wfs: [
    {
      name: 'creditDecision',
      states: [ 'internalBlacklist', 'decisionEngine', 'satQuery', 'manualDecision', 'approved', 'rejected' ],
      withActionHistory: { id: 2, state: 'approved' }
    },
    { name: 'processCreditApplication' },
    { name: 'withdrawLoan' }
  ]
};

fixtures.nbankNflowDev = {
  wfs: [
    {
      name: 'ConstantWorkflow',
      states: [ 'start', 'quickState', 'retryTwiceState', 'scheduleState', 'slowState', 'end', 'error' ],
      withActionHistory: { id: 467, state: 'end' }
    },
    { name: 'NoDelaysWorkflow' }
  ]
};

//module.exports = fixtures.nbankNflow;
module.exports = fixtures.nbankNflowDev;
