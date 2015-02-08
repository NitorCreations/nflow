'use strict';

describe('Controller: FrontPageCtrl', function () {

  // load the controller's module
  beforeEach(module('nflowVisApp.frontPage'));

  var FrontPageCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    FrontPageCtrl = $controller('FrontPageCtrl', {
      $scope: scope,
      WorkflowDefinitions: { query: function(){} }
    });
  }));

  it('should compile', function () {
    expect(true).toBe(true);
  });
});
