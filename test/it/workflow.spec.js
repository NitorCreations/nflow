'use strict';

var po = require('./pageobjects/pageobjects.js');

describe('workflow instance page', function () {

  var page = po.workflowPage({});
  var definitionPage = po.workflowDefinitionPage({});

  beforeEach(function () {
    page.get(2); // TODO this is not robust, currently expected creditDecision (approved)
  });

  it('provides navigation to related workflow definition', function () {
    page.toDefinition();
    expect(definitionPage.isDisplayed()).toBeTruthy();
  });

  it('selecting node in graph hi-lights node', function() {
    var state = 'decisionEngine';
    expect(page.graph.isSelected(state)).toBeFalsy();
    page.graph.select(state);
    expect(page.graph.isSelected(state)).toBeTruthy();
  });

  it('clicking active state in info hi-lights node', function() {
    page.info.getActiveState().then(function(activeState) {
      expect(page.graph.isSelected(activeState)).toBeFalsy();
      page.info.clickActiveState();
      expect(page.graph.isSelected(activeState)).toBeTruthy();
    });
  });

  describe('tabs', function() {
    describe('active', function () {
      function assertActiveTab(isActionHistoryActive, isStateVariablesActive, isManageActive) {
        expect(page.tabs.actionHistory.isActive()).toBe(isActionHistoryActive);
        expect(page.tabs.stateVariables.isActive()).toBe(isStateVariablesActive);
        expect(page.tabs.manage.isActive()).toBe(isManageActive);
      }

      it('action history is active by default, rest can be activated by clicking', function () {
        assertActiveTab(true, false, false);

        page.tabs.stateVariables.activate();
        assertActiveTab(false, true, false);

        page.tabs.manage.activate();
        assertActiveTab(false, false, true);

        page.tabs.actionHistory.activate();
        assertActiveTab(true, false, false);
      });
    });

    describe('action history', function() {
      beforeEach(function () {
        page.tabs.actionHistory.activate();
      });

      it('clicking action hi-lights node corresponding to action state in graph', function() {
        page.tabs.actionHistory.getActions().then(function(actions){
          var id = actions[0].id;
          var state = actions[0].state;

          expect(page.graph.isSelected(state)).toBeFalsy();
          page.tabs.actionHistory.select(id);
          expect(page.graph.isSelected(state)).toBeTruthy();
        });
      });
    });
  });
});
