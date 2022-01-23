(function () {
  'use strict';

  var m = angular.module('nflowExplorer.components.constants', []);

  m.constant('WorkflowStateType', {
    START: 'start',
    MANUAL: 'manual',
    NORMAL: 'normal',
    END: 'end',
    ERROR: 'error'
  });

  m.constant('WorkflowInstanceStatus', {
    CREATED: 'created',
    IN_PROGRESS: 'inProgress',
    FINISHED: 'finished',
    MANUAL: 'manual',
    EXECUTING: 'executing'
  });
})();
