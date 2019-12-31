'use strict';

var baseFn = require('./base');
var po = require('./pageobjects');

module.exports = function (spec) {
  var that = baseFn(spec);

  spec.view = $('section.wd-workflow-definition');
  spec.instanceSearchByTypeLink = element(by.linkText('Search related workflow instances'));

  that.get = function (type) {
    browser.get('/#!/workflow-definition/' + type);
    expect(that.isDisplayed()).toBeTruthy();
  };

  that.toInstanceSearchByType = function() {
    spec.instanceSearchByTypeLink.click();
  };

  that.graph = po.graph({});
  that.tabs = tabs({});

  return that;
};

function tabs(spec) {
  var that = baseFn(spec);

  that.activeInstances = po.tab({ linkText: 'Active instances'} );
  that.allInstances = allInstances();
  that.workflowSettings = po.tab({ linkText: 'Workflow settings'} );
  that.radiator = radiator();

  return that;
}

function allInstances() {
  var spec = { linkText: 'All instances' };
  var that = po.tab(spec);

  that.select = function(state) {
    $('tr.wd-state-' + state).click();
  };

  that.isSelected = function(state) {
    return spec.hasClasses($('tr.wd-state-' + state), ['highlight']);
  };

  that.toInstanceSearch = function(state) {
    $('tr.wd-state-' + state + ' a').click();
  };

  return that;
}

function radiator() {
  var spec = { linkText: 'Radiator'};
  var that = po.tab(spec);

  that.isStateChartDisplayed = function() {
    return spec.isDisplayed($('#stateChart'));
  };

  that.isExecutionChartDisplayed = function() {
    return spec.isDisplayed($('#executionChart'));
  };

  return that;
}
