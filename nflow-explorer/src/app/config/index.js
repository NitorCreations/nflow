(function () {
  'use strict';

  angular.module('nflowExplorer.config', [
    'nflowExplorer.config.console',
    'nflowExplorer.config.routes',
    'nflowExplorer.config.notificationFilter',
  ])
  .constant('config', new Config());

})();
