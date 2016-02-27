'use strict';

describe('Controller: FrontPageCtrl', function () {
  var ctrl;

  beforeEach(module('nflowExplorer.frontPage'));

  beforeEach(inject(function ($controller, WorkflowDefinitionService) {
    sinon.stub(WorkflowDefinitionService, 'query', function(){  return ['definition' ]; });

    ctrl = $controller('FrontPageCtrl', { WorkflowDefinitions: WorkflowDefinitionService });
  }));

  afterEach(inject(function(WorkflowDefinitionService) {
    WorkflowDefinitionService.query.restore();
  }));

  // TODO fix test
  xit('sets definitions into view model', function () {
    expect(ctrl.definitions).toEqual(['definition']);
  });

});
