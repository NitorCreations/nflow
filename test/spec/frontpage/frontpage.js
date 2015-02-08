'use strict';

describe('Controller: FrontpageCtrl', function () {

  // load the controller's module
  beforeEach(module('nflowVisApp.frontpage'));

  var FrontpageCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    FrontpageCtrl = $controller('FrontpageCtrl', {
      $scope: scope,
      WorkflowDefinitions: { query: function(){} }
    });
  }));

  it('should compile', function () {
    expect(true).toBe(true);
  });
});
