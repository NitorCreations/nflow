'use strict';

describe('Directive: executorTable', function () {

  beforeEach(module('nflowExplorer.frontPage.executorTable'));
  beforeEach(module('nflowExplorer.karma.templates'));

  it('sets executors into view model', inject(function ($rootScope, $compile) {
    var elem = $compile('<executor-table executors="expected"></executor-table>')($rootScope);
    $rootScope.$apply(function() { $rootScope.expected = ['foo', 'bar']; });

    var ctrl = elem.isolateScope().ctrl;
    expect(ctrl.executors).toEqual(['foo', 'bar']);
  }));

  describe('ExecutorTableCtrl: executorClass', function () {
    var ctrl;

    var today = moment();
    var yesterday = today.clone().subtract(1, 'days');
    var dayBeforeYesterday = today.clone().subtract(2, 'days');
    var tomorrow = today.clone().add(1, 'days');

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
