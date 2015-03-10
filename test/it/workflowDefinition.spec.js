'use strict';

var po = require('./pageobjects/pageobjects');

describe('workflow definition page', function () {

  var page = po.workflowDefinitionPage({});
  var searchPage = po.searchPage({});

  beforeEach(function() { page.get('creditDecision'); });

  it('provides navigation to instance search by type', function () {
    page.toInstanceSearchByType();
    expect(searchPage.isDisplayed()).toBeTruthy();

    expect(searchPage.getType()).toBe('creditDecision');
    expect(searchPage.getState()).toBe('');
    expect(searchPage.getInstanceId()).toBe('');
    expect(searchPage.getBusinessKey()).toBe('');
    expect(searchPage.getExternalId()).toBe('');

    expect(searchPage.hasResults()).toBeTruthy();
  });

  it('selecting node in graph hi-lights node', function() {
    var state = 'decisionEngine';
    expect(page.graph.isSelected(state)).toBeFalsy();
    page.graph.select(state);
    expect(page.graph.isSelected(state)).toBeTruthy();
  });

  describe('tabs', function() {
    describe('active', function() {
      function assertActiveTab(isActiveInstancesActive, isAllInstancesActive, isWorkflowSettingsActive,
                               isRadiatorActive) {
        expect(page.tabs.activeInstances.isActive()).toBe(isActiveInstancesActive);
        expect(page.tabs.allInstances.isActive()).toBe(isAllInstancesActive);
        expect(page.tabs.workflowSettings.isActive()).toBe(isWorkflowSettingsActive);
        expect(page.tabs.radiator.isActive()).toBe(isRadiatorActive);
      }

      it('all instances is active by default, rest can be activated by clicking', function () {
        assertActiveTab(true, false, false, false);

        page.tabs.allInstances.activate();
        assertActiveTab(false, true, false, false);

        page.tabs.workflowSettings.activate();
        assertActiveTab(false, false, true, false);

        page.tabs.radiator.activate();
        assertActiveTab(false, false, false, true);

        page.tabs.activeInstances.activate();
        assertActiveTab(true, false, false, false);
      });
    });

    describe('all instances', function() {
      beforeEach(function () {
        page.tabs.allInstances.activate();
      });

      it('clicking instance hi-lights corresponding node in graph', function() {
        var state = 'satQuery';
        expect(page.graph.isSelected(state)).toBeFalsy();
        page.tabs.allInstances.select(state);
        expect(page.graph.isSelected(state)).toBeTruthy();
      });

      it('provides navigation to instance search by type and state', function() {
        page.tabs.allInstances.toInstanceSearch('approved');
        expect(searchPage.isDisplayed()).toBeTruthy();

        expect(searchPage.getType()).toBe('creditDecision');
        expect(searchPage.getState()).toBe('approved');
        expect(searchPage.getInstanceId()).toBe('');
        expect(searchPage.getBusinessKey()).toBe('');
        expect(searchPage.getExternalId()).toBe('');

        expect(searchPage.hasResults()).toBeTruthy();
      });
    });

    describe('radiator', function() {
      beforeEach(function () {
        page.tabs.radiator.activate();
      });

      it('has state and execution charts', function () {
        expect(page.tabs.radiator.isStateChartDisplayed()).toBeTruthy();
        expect(page.tabs.radiator.isExecutionChartDisplayed()).toBeTruthy();
      });
    });
  });

});
