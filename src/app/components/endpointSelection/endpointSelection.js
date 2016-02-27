(function () {
  'use strict';

  var m = angular.module('nflowExplorer.components.endpointSelection', []);

  m.directive('endpointSelection', function endpointSelection() {
    return {
      scope: {},
      bindToController: true,
      restrict: 'E',
      controller: 'EndpointSelectionCtrl as ctrl',
      templateUrl: 'app/components/endpointSelection/endpointSelection.html'
    };
  });

  m.controller('EndpointSelectionCtrl', function EndpointSelectionCtrl(config, EndpointService) {
    var ctrl = this;
    ctrl.endpoints = config.nflowEndpoints;
    ctrl.selectedEndpointId = EndpointService.currentEndpoint().id;
    ctrl.endpointChange = function(id) {
      EndpointService.selectEndpoint(id);
    };
  });

})();
