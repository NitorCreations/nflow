'use strict';

describe('Directive: executorTable', function () {

  beforeEach(module('nflowVisApp.frontPage.executorTable'));

  it('sets executors into view model', inject(function ($rootScope, $compile, $templateCache) {
    $templateCache.put('app/front-page/executorTable.html', '');

    var elem = $compile('<executor-table executors="expected"></executor-table>')($rootScope);
    $rootScope.$apply(function() { $rootScope.expected = ['foo', 'bar']; });

    var ctrl = elem.isolateScope().ctrl;
    expect(ctrl.executors).toEqual(['foo', 'bar']);
  }));

  describe('ExecutorTableCtrl: executorClass', function () {
    var ctrl;

    var dayBeforeYesterday = moment().subtract(2, 'days');
    var yesterday = moment().subtract(1, 'days');
    var today = moment();
    var tomorrow = moment().add(1, 'days');

    beforeEach(inject(function ($controller) {
      ctrl = $controller('ExecutorTableCtrl');
    }));

    it('is undefined for dead executor', function () {
      expect(ctrl.executorClass({ active: dayBeforeYesterday, expires: today },  today)).toBeUndefined();
    });

    it('is "warning" for expired executor', function () {
      expect(ctrl.executorClass({ active: yesterday, expires: yesterday },  today)).toBe('warning');
    });

    it('is "success" for alive executor', function () {
      expect(ctrl.executorClass({ active: today, expires: today },  today)).toBe('success');
      expect(ctrl.executorClass({ active: today, expires: tomorrow },  today)).toBe('success');
    });
  });
});
