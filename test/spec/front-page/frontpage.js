'use strict';

describe('Controller: FrontPageCtrl', function () {
  var ctrl;

  beforeEach(module('nflowExplorer.frontPage'));

  beforeEach(inject(function ($controller, WorkflowDefinitions) {
    sinon.stub(WorkflowDefinitions, 'query', function(){  return ['definition' ]; });

    ctrl = $controller('FrontPageCtrl', { WorkflowDefinitions: WorkflowDefinitions });
  }));

  afterEach(inject(function(WorkflowDefinitions) {
    WorkflowDefinitions.query.restore();
  }));

  it('sets definitions into view model', function () {
    expect(ctrl.definitions).toEqual(['definition']);
  });

});
