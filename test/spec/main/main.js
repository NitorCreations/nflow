'use strict';

describe('Controller: MainCtrl', function () {

  // load the controller's module
  beforeEach(module('nflowVisApp.main'));

  var MainCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();
    MainCtrl = $controller('MainCtrl', {
      $scope: scope,
      WorkflowDefinitions: { query: function(){} }
    });
  }));

  it('should compile', function () {
    expect(true).toBe(true);
  });
});
