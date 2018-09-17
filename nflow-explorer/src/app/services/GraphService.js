(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.GraphService', [
    'nflowExplorer.config',
  ]);

  m.service('GraphService', function GraphServiceFactory(config, $q, $http, $rootScope) {
    this.loadCss = function getCss() {
      var defer = $q.defer();
      // links are relative to displayed page
      $http.get('styles/data/graph.css', {withCredentials: !!config.withCredentials})
        .then(function(response) {
          $rootScope.graph = {};
          $rootScope.graph.css = response.data;
          defer.resolve();
        }, function() {
          console.warn('Failed to load graph.css');
          $rootScope.graph = {};
          defer.resolve();
        });
      return defer.promise;
    };
  });

})();
