'use strict';

describe('Directive: definitionList', function () {

  beforeEach(module('nflowVisApp.frontPage.definitionList'));

  it('initializes definitions', inject(function ($rootScope, $compile, $templateCache) {
    $templateCache.put('app/front-page/definitionList.html', '');

    $rootScope.expected = ['foo', 'bar'];
    var elem = $compile('<definition-list definitions="expected"></definition-list>')($rootScope);
    $rootScope.$apply();

    var directiveScope = elem.isolateScope();
    expect(directiveScope.ctrl.definitions).toEqual(['foo', 'bar']);
  }));
});
