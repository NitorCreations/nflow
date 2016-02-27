'use strict';

describe('Directive: searchForm', function () {

  beforeEach(module('nflowExplorer.search.searchForm'));
  beforeEach(module('nflowExplorer.karma.templates'));

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
      WorkflowService;

    beforeEach(inject(function (_$controller_, _CriteriaModel_, _WorkflowService_) {
      $controller = _$controller_;
      CriteriaModel = _CriteriaModel_;
      WorkflowService = _WorkflowService_;
    }));

    function getCtrl(WorkflowSearch) {
      return $controller('SearchFormCtrl', { CriteriaModel: CriteriaModel, WorkflowService: WorkflowService});
    }

    it('sets criteria model into view model', function () {
      var expected = CriteriaModel.model = { foo: 'bar' };

      var ctrl = getCtrl(WorkflowService);
      expect(ctrl.model).toEqual(expected);
    });

    it('sets instance statuses into view model', function () {
      expect(getCtrl(WorkflowService).instanceStatuses).toEqual([ 'created', 'inProgress', 'finished', 'manual' ]);
    });

    it('with empty criteria does not trigger search', function () {
      CriteriaModel.model = {};
      var spy = sinon.spy(WorkflowService, 'query');

      getCtrl(WorkflowService);

      expect(spy.callCount).toBe(0);
      WorkflowService.query.restore();
    });

    it('with non-empty criteria does not trigger search', function () {
      CriteriaModel.model = { foo: 'bar' };
      var spy = sinon.spy(WorkflowService, 'query');

      getCtrl(WorkflowService);

      expect(spy.callCount).toBe(1);
      WorkflowService.query.restore();
    });

    describe('search', function () {
      var $httpBackend,
        url;

      beforeEach(inject(function (_$httpBackend_, config) {
        $httpBackend = _$httpBackend_;

        CriteriaModel.model = { foo: 'bar' };
        url = config.nflowUrl + '/v1/workflow-instance?foo=bar';
      }));

      afterEach(function() {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
      });

      it('sets result into view model', function () {
        $httpBackend.whenGET(url).respond(200, [ 'expected' ]);
        $httpBackend.expectGET(url);
        var ctrl = getCtrl(WorkflowService);
        $httpBackend.flush();

        expect(angular.copy(ctrl.results)).toEqual([ 'expected' ]);
      });

      it('indicator is shown if search takes more than 500 ms', inject(function ($timeout) {
        $httpBackend.whenGET(url).respond(200, []);

        var ctrl = getCtrl(WorkflowService);
        expect(ctrl.showIndicator).toBeFalsy();

        $timeout.flush(100);
        expect(ctrl.showIndicator).toBeFalsy();
        $httpBackend.flush();
        expect(ctrl.showIndicator).toBeFalsy();

        ctrl.search();
        $timeout.flush(499);
        expect(ctrl.showIndicator).toBeFalsy();
        $timeout.flush(1);
        expect(ctrl.showIndicator).toBeTruthy();
        $httpBackend.flush();
        expect(ctrl.showIndicator).toBeFalsy();

        $timeout.flush();
        $timeout.verifyNoPendingTasks();
      }));
    });
  });

});
