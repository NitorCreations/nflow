(function () {
  'use strict';
  var m = angular.module('nflowExplorer.config.notificationFilter', ['toastr']);

  m.service('httpNotificationInterceptor', ['$q', '$injector', function($q, $injector) {
    return {
      responseError: function(rejection) {
        var toastr = $injector.get('toastr');
        var toastrConfig = {
          timeOut: 0
        };
        if (rejection.status === -1) {
          toastr.error('Check network connection, CORS settings and ensure that nFlow REST API is running',
              'REST API request aborted by browser', toastrConfig);
        } else if (rejection.status === 401 || rejection.status === 403) {
          toastr.error('Authentication failed', toastrConfig);
        } else if (rejection.status === 404) {
          toastr.error('Check that your URL is valid', 'Page not found', toastrConfig);
        } else if (rejection.status >= 500) {
          toastr.error('Internal server error', toastrConfig);
        }
        return $q.reject(rejection);
      }
    };
  }]);

  m.config(function($httpProvider) {
    $httpProvider.interceptors.push('httpNotificationInterceptor');
  });
})();
