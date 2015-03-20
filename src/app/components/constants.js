(function () {
  'use strict';

  var m = angular.module('nflowExplorer.constants', []);

  m.constant('WorkflowStateType', {
    START: 'start',
    MANUAL: 'manual',
    NORMAL: 'normal',
    END: 'end',
    ERROR: 'error'
  });
})();
