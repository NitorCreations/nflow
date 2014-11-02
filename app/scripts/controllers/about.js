'use strict';

/**
 * @ngdoc function
 * @name nflowVisApp.controller:AboutCtrl
 * @description
 * # AboutCtrl
 * Controller of the nflowVisApp
 */
angular.module('nflowVisApp')
.controller('AboutCtrl', function ($scope, config) {
  $scope.nflowUrl = function() {
    return config.nflowUrl;
  };

  $scope.nflowApiDocs = function() {
    return config.nflowUrl + '/ui/';
  };
});
