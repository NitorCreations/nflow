'use strict';

describe('Directive: searchForm', function () {

  beforeEach(module('nflowVisApp.search.searchForm'));
  beforeEach(module('nflowVisApp.karma.templates'));

  it('sets results and definitions into view model', inject(function ($rootScope, $compile) {
    var elem = $compile('<search-form results="results" definitions="definitions"></search-form>')($rootScope);
    $rootScope.$apply(function() {
      $rootScope.results = ['foo'];
      $rootScope.definitions = ['bar'];
    });

    var ctrl = elem.isolateScope().ctrl;
    expect(ctrl.results).toEqual(['foo']);
    expect(ctrl.definitions).toEqual(['bar']);
  }));

  describe('SearchFormCtrl', function () {
    var $controller,
      CriteriaModel,
      WorkflowSearch;

    beforeEach(inject(function (_$controller_, _CriteriaModel_, _WorkflowSearch_) {
      $controller = _$controller_;
      CriteriaModel = _CriteriaModel_;
      WorkflowSearch = _WorkflowSearch_;
    }));

    function getCtrl(WorkflowSearch) {
      return $controller('SearchFormCtrl', { CriteriaModel: CriteriaModel, WorkflowSearch: WorkflowSearch});
    }

    it('sets criteria model into view model', function () {
      var expected = CriteriaModel.model = { foo: 'bar' };

      var ctrl = getCtrl(WorkflowSearch);
      expect(ctrl.model).toEqual(expected);
    });

    it('with empty criteria does not trigger search', function () {
      CriteriaModel.model = {};
      var spy = sinon.spy(WorkflowSearch, 'query');

      getCtrl(WorkflowSearch);

      expect(spy.callCount).toBe(0);
      WorkflowSearch.query.restore();
    });

    it('with non-empty criteria does not trigger search', function () {
      CriteriaModel.model = { foo: 'bar' };
      var spy = sinon.spy(WorkflowSearch, 'query');

      getCtrl(WorkflowSearch);

      expect(spy.callCount).toBe(1);
      WorkflowSearch.query.restore();
    });

    describe('search', function () {
      it('sets result into view model', function () {
        sinon.stub(WorkflowSearch, 'query', function() { return [ 'result' ]; });

        CriteriaModel.model = { foo: 'bar' };
        var ctrl = getCtrl(WorkflowSearch);
        expect(ctrl.results).toEqual([ 'result' ]);

        WorkflowSearch.query.restore();
      });
    });

  });

});
