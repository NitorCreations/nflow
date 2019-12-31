'use strict';

  var baseFn = require('./base');
  var po = require('./pageobjects');

module.exports = function (spec) {
  var that = baseFn(spec);

  spec.view = $('section.wd-workflow');
  spec.definitionLink = element(by.linkText('Go to workflow definition'));

  that.get = function (id) {
    browser.get('/#!/workflow/' + id);
    expect(that.isDisplayed()).toBeTruthy();
  };

  that.toDefinition = function() {
    spec.definitionLink.click();
  };

  that.info = info({});
  that.graph = po.graph({});
  that.tabs = tabs({});

  return that;
};

function info(spec) {
  var that = baseFn(spec);
  spec.activeState = $('.wd-workflow-info .wd-active-state');

  that.getActiveState = function() {
    return spec.activeState.getText();
  };

  that.clickActiveState = function() {
    spec.activeState.click();
  };

  return that;
}

function tabs(spec) {
  var that = baseFn(spec);

  that.actionHistory = actionHistory();
  that.stateVariables = po.tab({ linkText: 'State variables'} );
  that.manage = po.tab({ linkText: 'Manage'} );

  return that;
}

function actionHistory() {
  var spec = {linkText: 'Action history'};
  spec.actions = $$('table#action-history tbody tr');

  var that = po.tab(spec);

  that.getActions = function() {
    function colText(action, idx) { return action.$$('td').get(idx).getText(); }

    return spec.actions.then(function(actions) {
      var results = [];
      for (var i=0; i < actions.length; i++) {
        var action = actions[i];

        var id = colText(action, 0);
        var state = colText(action, 1);

        results.push(protractor.promise.all([ id, state ]).then(function(resolved){
          return { id: resolved[0], state: resolved[1] };
        }));
      }
      return protractor.promise.all(results);
    });
  };

  that.select = function(id){
    return $('tr.wd-action-' + id).click();
  };

  return that;
}
