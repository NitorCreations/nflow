(function () {
  'use strict';
  var m = angular.module('nflowExplorer.config.notificationFilter', ['toastr']);

  m.service('httpNotificationInterceptor', ['$q', '$injector', function($q, $injector) {
    return {
      responseError: function(rejection) {
        var toastr = $injector.get('toastr');
        if (rejection.status === -1) {
          toastr.error('Check network connection and CORS settings', 'REST API request aborted by browser', {
            timeOut: 0
          });
        } else if (rejection.status === 401 || rejection.status === 403) {
          toastr.error('Authentication failed', {
            timeOut: 0
          });
        } else if (rejection.status === 404) {
          toastr.error('Check that your URL is valid', 'Page not found', {
            timeOut: 0
          });
        } else if (rejection.status >= 500) {
          toastr.error('Internal server error', {
            timeOut: 0
          });
        }
        return $q.reject(rejection);
      }
    };
  }]);

  m.config(function($httpProvider) {
    $httpProvider.interceptors.push('httpNotificationInterceptor');
  });
})();
