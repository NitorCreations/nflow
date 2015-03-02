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

  that.actionHistory = tabBase({ linkText: 'Action history'} );
  that.actionHistory.getActions = function() {
    function colText(action, idx) { return action.$$('td').get(idx).getText(); }

    return $$('table#action-history tbody tr').then(function(actions) {
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

  that.actionHistory.select = function(id){
    $('tr.wd-action-' + id).click();
  };

  that.stateVariables = tabBase({ linkText: 'State variables'} );
  that.manage = tabBase({ linkText: 'Manage'} );

  return that;
}

module.exports = function (spec) {
  var that = require('./base')(spec);

  spec.view = $('section.wd-workflow');
  spec.definitionLink = element(by.linkText('Go to workflow definition'));

  that.get = function (id) {
    browser.get('/#/workflow/' + id);
    expect(that.isDisplayed()).toBeTruthy();
  };

  that.toDefinition = function() {
    spec.definitionLink.click();
  };

  that.graph = graph({});
  that.tabs = tabs({});

  return that;
};
