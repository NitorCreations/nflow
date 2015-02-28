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

  return that;
};


