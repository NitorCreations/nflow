'use strict';

describe('Controller: ExecutorsCtrl', function () {
  var ctrl;

  beforeEach(module('nflowExplorer.executors'));

  beforeEach(inject(function ($controller) {
    ctrl = $controller('ExecutorsCtrl', { ExecutorService: { executors: ['executor'] } }); }));

  it('sets executors into view model', function () {
    expect(ctrl.executors).toEqual(['executor']);
  });

});
