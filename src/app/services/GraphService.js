(function () {
  'use strict';

  var m = angular.module('nflowExplorer.services.GraphService', []);

  m.service('GraphService', function GraphServiceFactory($q, $http, $rootScope) {
    this.loadCss = function getCss() {
      var defer = $q.defer();
      // links are relative to displayed page
      $http.get('styles/data/graph.css')
        .success(function(data) {
          $rootScope.graph = {};
          $rootScope.graph.css=data;
          defer.resolve();
        })
        .error(function() {
          console.warn('Failed to load graph.css');
          $rootScope.graph = {};
          defer.resolve();
        });
      return defer.promise;
    };
  });

})();
