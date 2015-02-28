'use strict';

function graph(spec) {
  var that = require('./base')(spec);

  that.isSelected = function(nodeId) {
    return spec.hasClasses(nodeIdSelector(nodeId), ['selected']);
  };

  that.select = function(state) {
    nodeIdSelector(state).click();
  };

  return that;

  function nodeIdSelector(nodeId) {
    return $('#node_' + nodeId);
  }
}

function tabBase(spec) {
  spec.link = element(by.linkText(spec.linkText));

  var that = require('./base')(spec);

  that.isActive = function() {
    return spec.hasClasses(spec.parent(spec.link), ['active']);
  };

  that.activate = function() {
    spec.link.click();
  };

  return that;
}

function tabs(spec) {
  var that = require('./base')(spec);

  that.activeInstances = tabBase({ linkText: 'Active instances'} );

  that.allInstances = tabBase({linkText: 'All instances' });
  that.allInstances.select = function(state){
    $('tr.wd-state-' + state).click();
  };

  that.workflowSettings = tabBase({ linkText: 'Workflow settings'} );

  that.radiator = tabBase({ linkText: 'Radiator'} );

  return that;
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


