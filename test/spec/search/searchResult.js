'use strict';

describe('Directive: searchResult', function () {
  var definitions,
    results;

  var backlog = { type: 'foo', state: 'backlog' },
    inProgress = { type: 'foo', state: 'inProgress' },
    blocked = { type: 'foo', state: 'blocked' },
    review = { type: 'foo', state: 'review' },
    done = { type: 'foo', state: 'done' },
    testFailed = { type: 'foo', state: 'testFailed' },
    sapReady = { type: 'foo', state: 'sapReady' };

  beforeEach(module('nflowVisApp.search.searchResult'));
  beforeEach(module('nflowVisApp.karma.templates'));

  beforeEach(function () {
    results = [ backlog, inProgress, blocked, review, done, testFailed, sapReady ];
    definitions = [{ type: 'foo', states: [
      { id: 'backlog', type: 'start' },
      { id: 'inProgress', type: 'normal' },
      { id: 'blocked', type: 'manual' },
      { id: 'review', type: 'manual' },
      { id: 'done', type: 'end' },
      { id: 'testFailed', type: 'error' },
      { id: 'sapReady', type: 'unknown' }
    ], onError: 'blocked' }];
  });


  it('sets results and definitions into view model', inject(function ($rootScope, $compile) {
    var elem = $compile('<search-result results="results" definitions="definitions"></search-result>')($rootScope);
    $rootScope.$apply(function() {
      $rootScope.results = _.cloneDeep(results);
      $rootScope.definitions = _.cloneDeep(definitions);
    });

    var ctrl = elem.isolateScope().ctrl;
    expect(_.map(ctrl.results, _.partialRight(_.omit, '$$hashKey'))).toEqual(results);
    expect(ctrl.definitions).toEqual(definitions);
  }));

  describe('SearchResultCtrl: getStateClass', function () {
    var ctrl;

    beforeEach(inject(function ($controller) {
      ctrl = $controller('SearchResultCtrl');
      ctrl.results = results;
      ctrl.definitions = definitions;
    }));

    it('non-value result -> ""', function () {
      expect(ctrl.getStateClass(undefined)).toEqual('');
      expect(ctrl.getStateClass(null)).toEqual('');
    });

    it('unknown definition -> ""', function () {
      var unknown = _.assign(_.clone(backlog), { type: 'unknown' });
      expect(ctrl.getStateClass(unknown)).toEqual('');
    });

    it('error state -> danger', function () {
      expect(ctrl.getStateClass(blocked)).toEqual('danger');
    });

    it('start state -> ""', function () {
      expect(ctrl.getStateClass(backlog)).toEqual('');
    });

    it('normal state -> info', function () {
      expect(ctrl.getStateClass(inProgress)).toEqual('info');
    });

    it('manual state -> warning', function () {
      expect(ctrl.getStateClass(review)).toEqual('warning');
    });

    it('end state -> success', function () {
      expect(ctrl.getStateClass(done)).toEqual('success');
    });

    it('error state -> danger', function () {
      expect(ctrl.getStateClass(testFailed)).toEqual('danger');
    });

    it('unknown state -> ""', function () {
      expect(ctrl.getStateClass(sapReady)).toEqual('');
    });
  });

});
