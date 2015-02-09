'use strict';

describe('Directive: definitionList', function () {

  beforeEach(module('nflowVisApp.frontPage.definitionList'));

  it('sets definitions into view model', inject(function ($rootScope, $compile, $templateCache) {
    $templateCache.put('app/front-page/definitionList.html', '');

    var elem = $compile('<definition-list definitions="expected"></definition-list>')($rootScope);
    $rootScope.$apply(function() { $rootScope.expected = ['foo', 'bar']; });

    var ctrl = elem.isolateScope().ctrl;
    expect(ctrl.definitions).toEqual(['foo', 'bar']);
  }));
});
