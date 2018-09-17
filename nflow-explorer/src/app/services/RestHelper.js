(function () {
  'use strict';
  var m = angular.module('nflowExplorer.services.RestHelper', [
    'nflowExplorer.config',
    'ngResource',
  ]);

  m.factory('RestHelper', function RestHelper(config, $resource) {
    var helpers = {};

    helpers.query = function (query, params) {
      return $resource(config.nflowUrl + query.path, params, {
        'query': {
          method: 'GET',
          isArray: true,
          cache: query.cache,
          withCredentials: !!config.withCredentials
        }
      }).query().$promise;
    };

    helpers.get = function (query, params, paramDefaults) {
      return $resource(config.nflowUrl + query.path, paramDefaults, {
        'query': {
          method: 'GET',
          cache: query.cache,
          withCredentials: !!config.withCredentials
        }
      }).get(params).$promise;
    };

    helpers.update = function (path, data) {
      return $resource(config.nflowUrl + path, null, {
        'update': {
          method: 'PUT',
          withCredentials: !!config.withCredentials
        }
      }).update(data).$promise;
    };

    return helpers;
  });
})();
