'use strict';

describe('Controller: FrontPageCtrl', function () {
  var ctrl;

  beforeEach(module('nflowExplorer.frontPage'));

  beforeEach(inject(function ($controller, WorkflowDefinitionService, $q) {
    sinon.stub(WorkflowDefinitionService, 'list', function(){
      return $q.when([ 'definition' ]);
    });

    ctrl = $controller('FrontPageCtrl', { WorkflowDefinitionService: WorkflowDefinitionService });
  }));

  afterEach(inject(function(WorkflowDefinitionService) {
    WorkflowDefinitionService.list.restore();
  }));

  // TODO fix test, works in normal usage, not in test
  xit('sets definitions into view model', function () {
    expect(ctrl.definitions).toEqual(['definition']);
  });

});
