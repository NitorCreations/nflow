(function () {
  'use strict';

  var m = angular.module('nflowExplorer.components.svgPanZoom', []);

  m.factory('svgPanZoom', function SvgPanZoomFactory($window) {
    if (!$window.svgPanZoom) {
      console.error('Failed to load svgPanZoom');
      return function() {};
    }
    return $window.svgPanZoom;
  });

})();
