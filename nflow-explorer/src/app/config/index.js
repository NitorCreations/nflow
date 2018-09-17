(function () {
  'use strict';

  angular.module('nflowExplorer.config', [
    'nflowExplorer.config.console',
    'nflowExplorer.config.routes',
  ])
  .constant('config', new Config());

})();
