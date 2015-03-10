'use strict';

module.exports.frontPage = require('./frontPage');
module.exports.workflowDefinitionPage = require('./workflowDefinitionPage');
module.exports.workflowPage = require('./workflowPage');
module.exports.searchPage = require('./searchPage');

module.exports.menu = require('./menu');

module.exports.graph = function (spec) {
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
};

module.exports.tab = function tab(spec) {
  spec.link = element(by.linkText(spec.linkText));

  var that = require('./base')(spec);

  that.isActive = function() {
    return spec.hasClasses(spec.parent(spec.link), ['active']);
  };

  that.activate = function() {
    spec.link.click();
  };

  return that;
};
