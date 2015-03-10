'use strict';

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.definitions = element(by.linkText('Workflow definitions'));
  spec.instances = element(by.linkText('Workflow instances'));
  spec.about = element(by.linkText('About'));

  spec.isActive = function(link) {
    return spec.hasClasses(spec.parent(link), ['active']);
  };

  that.toDefinitions = function() {
    return spec.definitions.click();
  };

  that.toInstances = function() {
    return spec.instances.click();
  };

  that.toAbout = function() {
    return spec.about.click();
  };

  that.isDefinitionsActive = function() {
    return spec.isActive(spec.definitions);
  };

  that.isInstancesActive = function() {
    return spec.isActive(spec.instances);
  };

  that.isAboutActive = function() {
    return spec.isActive(spec.about);
  };

  return that;
};
