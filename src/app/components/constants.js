(function () {
  'use strict';

  var m = angular.module('nflowVisApp.contants', []);

  m.constant('WorkflowStateType', {
    START: 'start',
    MANUAL: 'manual',
    NORMAL: 'normal',
    END: 'end',
    ERROR: 'error'
  });
})();
