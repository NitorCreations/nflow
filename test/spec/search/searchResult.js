'use strict';

describe('Directive: searchResult', function () {

  beforeEach(module('nflowVisApp.search.searchResult'));
  beforeEach(module('nflowVisApp.karma.templates'));

  it('sets results and definitions into view model', inject(function ($rootScope, $compile) {
    var elem = $compile('<search-result results="results" definitions="definitions"></search-result>')($rootScope);
    $rootScope.$apply(function() {
      $rootScope.results = [{ type: 'foo', state: 'normal' }];
      $rootScope.definitions = [ { type: 'foo', states: [ { id: 'normal' }]}];
    });

    var ctrl = elem.isolateScope().ctrl;
    expect(_.map(ctrl.results, _.partialRight(_.omit, '$$hashKey'))).toEqual([{ type: 'foo', state: 'normal' }]);
    expect(ctrl.definitions).toEqual([ { type: 'foo', states: [ { id: 'normal' }]}]);
  }));
});
