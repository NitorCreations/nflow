(function () {
  'use strict';

  var m = angular.module('nflowVisApp.constants', []);

  m.constant('WorkflowStateType', {
    START: 'start',
    MANUAL: 'manual',
    NORMAL: 'normal',
    END: 'end',
    ERROR: 'error'
  });
})();
