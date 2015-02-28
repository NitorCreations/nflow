'use strict';

function graph(spec) {
  var that = require('./base')(spec);

  that.isSelected = function(nodeId) {
    return spec.hasClasses(nodeIdSelector(nodeId), ['selected']);
  };

  that.select = function(nodeId) {
    nodeIdSelector(nodeId).click();
  };

  return that;

  function nodeIdSelector(nodeId) {
    return $('#node_' + nodeId);
  }
}

function tabs(spec) {
  var that = require('./base')(spec);

  spec.activeInstances = element(by.linkText('Active instances'));
  spec.allInstances = element(by.linkText('All instances'));
  spec.workflowSettings = element(by.linkText('Workflow settings'));
  spec.radiator = element(by.linkText('Radiator'));

  that.activateActiveInstances = function() { spec.activeInstances.click(); };
  that.activateAllInstances = function() { spec.allInstances.click(); };
  that.activateWorkflowSettings = function() { spec.workflowSettings.click(); };
  that.activateRadiator = function() { spec.radiator.click(); };

  that.isActiveInstancesActive = function() { return isActive(spec.activeInstances); };
  that.isAllInstancesActive = function() { return isActive(spec.allInstances); };
  that.isWorkflowSettingsActive = function() { return isActive(spec.workflowSettings); };
  that.isRadiatorActive = function() { return isActive(spec.radiator); };

  return that;

  function isActive(link) { return spec.hasClasses(spec.parent(link), ['active']); }
}

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.view = $('section.wd-workflow-definition');
  spec.instanceSearchByTypeLink = by.linkText('Search related workflow instances');

  that.get = function (type) {
    browser.get('/#/workflow-definition/' + type);
    expect(that.isDisplayed()).toBeTruthy();
  };

  that.toInstanceSearchByType = function() {
    element(spec.instanceSearchByTypeLink).click();
  };

  that.graph = graph({});
  that.tabs = tabs({});

  return that;
};


