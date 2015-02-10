'use strict';

describe('Directive: searchForm', function () {

  beforeEach(module('nflowVisApp.search.searchForm'));

  describe('Service: SearchFormService', function () {
    var SearchFormService;

    beforeEach(inject(function (_SearchFormService_) {
      SearchFormService = _SearchFormService_;
    }));

    it('reference to results is not overwritten on search', inject(function ($httpBackend, config) {
      $httpBackend.whenGET(config.nflowUrl + '/v1/workflow-instance').respond(200, [ 'expected' ]);

      var actual = SearchFormService.results;

      expect(actual).toEqual([]);

      SearchFormService.search({});
      $httpBackend.flush();

      expect(actual).toEqual(['expected']);
    }));

    describe('input criteria', function () {
      var mock, expectation;
      beforeEach(inject(function (WorkflowSearch) {
        mock = sinon.mock(WorkflowSearch);
        expectation = mock.expects('query');
      }));

      afterEach(function() {
        mock.restore();
      });

      it('forwards empty criteria', function () {
        SearchFormService.search({});

        expectation.withExactArgs({});
        expectation.verify();
      });

      it('forwards type of type', function () {
        var t = {  type: 'foo' };
        SearchFormService.search({ type: t });

        expectation.withExactArgs(t);
        expectation.verify();
      });

      it('forwards name of state when there is no type', function () {
        var s = {  name: 'foo' };
        SearchFormService.search({ state: s });

        expectation.withExactArgs(s);
        expectation.verify();
      });

      it('forwards name of state when state is included in type', function () {
        SearchFormService.search({ type: { type: 'foo', states: [ { name: 'bar'} ] }, state: { name: 'bar'} });
        expectation.withExactArgs({ type: 'foo', state: 'bar' });
        expectation.verify();
      });

      it('forwards other than type and state', function () {
        var c = { foo: 'foo', bar: 'bar' };
        SearchFormService.search(c);

        expectation.withExactArgs(c);
        expectation.verify();
      });

      it('omits type without type and state without name', function () {
        SearchFormService.search({ type: 'foo', state: 'bar' });

        expectation.withExactArgs({});
        expectation.verify();
      });

      it('omits undefined and null', function () {
        SearchFormService.search({ foo: undefined, bar: null });

        expectation.withExactArgs({});
        expectation.verify();
      });

      it('omits states not in type', function () {
        SearchFormService.search({ type: { type: 'foo', states: [ { name: 'baz'} ] }, state: { name: 'bar'} });
        expectation.withExactArgs({ type: 'foo'});
        expectation.verify();
      });

      it('omits states not in type 2', function () {
        SearchFormService.search({ type: { type: 'foo' }, state: { name: 'bar'} });
        expectation.withExactArgs({ type: 'foo'});
        expectation.verify();
      });

    });

  });

});
