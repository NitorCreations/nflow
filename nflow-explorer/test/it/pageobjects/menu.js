'use strict';

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.definitions = element(by.linkText('Workflow definitions'));
  spec.instances = element(by.linkText('Workflow instances'));
  spec.executors = element(by.linkText('Executors'));
  spec.about = element(by.linkText('About'));
  spec.endpointSelection = element(by.css('.endpoint-selection .dropdown-toggle'));
  spec.endpointMenu = element(by.css('.endpoint-selection .dropdown-menu'));

  spec.isActive = function(link) {
    return spec.hasClasses(spec.parent(link), ['active']);
  };

  that.toDefinitions = function() {
    return spec.definitions.click();
  };

  that.toInstances = function() {
    return spec.instances.click();
  };

  that.toExecutors = function() {
    return spec.executors.click();
  };

  that.toAbout = function() {
    return spec.about.click();
  };

  that.clickEndpointSelection = function() {
    spec.endpointSelection.click();
  };

  that.isEnpointSelectionOpen = function() {
    return spec.endpointMenu.isDisplayed();
  };

  that.isDefinitionsActive = function() {
    return spec.isActive(spec.definitions);
  };

  that.isInstancesActive = function() {
    return spec.isActive(spec.instances);
  };

  that.isExecutorsActive = function() {
    return spec.isActive(spec.executors);
  };

  that.isAboutActive = function() {
    return spec.isActive(spec.about);
  };

  that.isEnpointSelectionPresent = function() {
    return spec.endpointSelection.isPresent();
  };

  that.selectedEndpoint = function() {
    return spec.endpointSelection.getText();
  };

  that.availableEndpoints = function() {
    return spec.endpointMenu.all(by.css('li')).map(function(elem) {
      return elem.getText();
    });
  };

  that.selectEndpoint = function(endpointId) {
    spec.endpointMenu.element(by.css('#select-endpoint-' + endpointId)).click();
  };

  return that;
};
