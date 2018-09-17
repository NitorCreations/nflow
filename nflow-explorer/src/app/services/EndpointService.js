(function () {
  'use strict';
  var m = angular.module('nflowExplorer.services.EndpointService', [
    'nflowExplorer.config',
  ]);
  m.service('EndpointService', function EndpointService(config, $window, $state) {
    var api = this;
    api.currentEndpoint = currentEndpoint;
    api.selectEndpoint = selectEndpoint;
    api.availableEndpoints = availableEndpoints;
    api.init = init;

    function currentEndpoint() {
      return config.selectedEndpoint;
    }

    function updateEndpoint(endpoint) {
      if(!endpoint) {
        return;
      }
      var change = config.selectedEndpoint && config.selectedEndpoint.id !== endpoint.id;
      config.selectedEndpoint = endpoint;
      config.nflowUrl = endpoint.apiUrl;
      config.nflowApiDocs = endpoint.docUrl;
      config.withCredentials = endpoint.withCredentials;

      try {
        $window.localStorage.setItem('nflow.endpoint', endpoint.id);
      } catch (e) {}

      if(change) {
        $state.go('frontPage');
        $window.location.reload();
      }
    }

    function selectEndpoint(endpointId) {
      if(!endpointId) {
        updateEndpoint(_.first(config.nflowEndpoints));
        return;
      }
      var storedEndpoint = _.find(api.availableEndpoints(), {id: endpointId});
      updateEndpoint(storedEndpoint);
    }

    function init() {
      var endpointId;
      try {
        endpointId = $window.localStorage.getItem('nflow.endpoint');
      } catch(e) {
        endpointId = $window.sessionStorage.getItem('nflow.endpoint');
      }
      selectEndpoint(endpointId);
    }

    function availableEndpoints() {
      return config.nflowEndpoints;
    }
  });
})();
