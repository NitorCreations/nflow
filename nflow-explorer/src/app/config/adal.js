(function () {
    'use strict';
    var m = angular.module('nflowExplorer.config.adal', ['nflowExplorer.config', 'AdalAngular']);
    m.config(function(config, $httpProvider, adalAuthenticationServiceProvider) {
        if (config.adal && config.adal.requireADLogin) {
            adalAuthenticationServiceProvider.init(config.adal, $httpProvider);
        }
    });
})();