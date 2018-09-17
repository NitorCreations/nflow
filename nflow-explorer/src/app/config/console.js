(function () {
  'use strict';

  angular.module('nflowExplorer.config.console', []).config(function(){
    // adapted from
    // http://stackoverflow.com/questions/3326650/console-is-undefined-error-for-internet-explorer/16916941#16916941

    var methods = [
      'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error',
      'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log',
      'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd',
      'timeStamp', 'trace', 'warn'
    ];
    var console = (window.console = window.console || {});
    _.forEachRight(methods, function(m) { if (!console[m]) { console[m] = _.noop; } });
  });

})();
