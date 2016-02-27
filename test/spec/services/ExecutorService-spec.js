'use strict';

describe('Service: ExecutorService', function () {
  var $httpBackend,
    $interval,
    ExecutorService,
    url,
    pollInterval;

  beforeEach(module('nflowExplorer.services.ExecutorService'));
  beforeEach(inject(function (_$httpBackend_, _$interval_, _ExecutorService_, config) {
    $httpBackend = _$httpBackend_;
    $interval = _$interval_;
    ExecutorService = _ExecutorService_;

    url = config.nflowUrl + '/v1/workflow-executor';
    pollInterval = config.radiator.pollPeriod * 1000;
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('initially has no executors', function () {
    expect(ExecutorService.executors).toEqual([]);
  });

  it('when started, executors are set and poll is started', function () {
    var backend = $httpBackend.whenGET(url).respond(200, [ 'expected' ]);

    // important: reference to executors must not be overwritten on poll update
    var actual = ExecutorService.executors;
    expect(actual).toEqual([]);

    $httpBackend.expectGET(url);
    ExecutorService.start();
    $httpBackend.flush();
    expect(actual).toEqual([ 'expected']);

    backend.respond(200, [ 'expected 2' ]);
    $httpBackend.expectGET(url);
    $interval.flush(pollInterval);
    $httpBackend.flush();
    expect(actual).toEqual([ 'expected 2']);

    backend.respond(200, [ 'expected 3' ]);
    $httpBackend.expectGET(url);
    $interval.flush(pollInterval);
    $httpBackend.flush();
    expect(actual).toEqual([ 'expected 3']);
  });

  it('after start subsequent calls to start do not start multiple pollers', inject(function (ExecutorService) {
    var spy = sinon.spy(ExecutorService, 'list');
    $httpBackend.whenGET(url).respond(200, []);

    ExecutorService.start();
    ExecutorService.start();
    $httpBackend.flush();

    expect(spy.callCount).toBe(1);

    ExecutorService.list.restore();
  }));
});
