'use strict';

describe('Controller: AboutCtrl', function () {

  // load the controller's module
  beforeEach(module('nflowExplorer.about'));

  var AboutCtrl,
    scope;

  // Initialize the controller and a mock scope
  beforeEach(inject(function ($controller, $rootScope) {
    scope = $rootScope.$new();

    AboutCtrl = $controller('AboutCtrl', {
      $scope: scope,
      config: {}
    });
  }));

  it('should compile', function () {
    expect(true).toBe(true);
  });
});
