'use strict';

var fixtures = {};

var demoWorkflow = {
  name: 'demo',
  states: [ 'begin', 'process', 'done', 'error' ],
  withActionHistory: { id: 1, state: 'done' }
};

fixtures.demoServer = {
  workflow: demoWorkflow,
  wfs: [
    { name: 'creditApplicationProcess' },
    demoWorkflow,
    { name: 'fibonacci' },
    { name: 'slowWorkflow' },
    { name: 'wordGenerator' },
    { name: 'wordGeneratorErrors' }
  ]
};

fixtures.nbankNflow = {
  wfs: [
    {
      name: 'creditDecision',
      states: [ 'internalBlacklist', 'decisionEngine', 'satQuery', 'manualDecision', 'approved', 'rejected' ],
      withActionHistory: { id: 2, state: 'done' }
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
module.exports = fixtures.demoServer;
