'use strict';

describe('Controller: FrontPageCtrl', function () {

  beforeEach(module('nflowVisApp.frontPage'));

  var ctrl;

  beforeEach(inject(function ($controller, WorkflowDefinitions) {
    sinon.stub(WorkflowDefinitions, 'query', function(){  return ['definition' ]; });

    ctrl = $controller('FrontPageCtrl', {
      WorkflowDefinitions: WorkflowDefinitions,
      ExecutorPoller: { executors: ['executor'] } });
  }));

  it('initializes definitions', function () {
    expect(ctrl.definitions).toEqual(['definition']);
  });

  it('initializes executors', function () {
    expect(ctrl.executors).toEqual(['executor']);
  });
});
