'use strict';

// definition, graph: verify clicks
// definition, tab:all instances: verify list

var po = require('./pageobjects/pageobjects');

describe('workflow definition page', function () {

  var page = po.workflowDefinitionPage({});
  var searchPage = po.searchPage({});

  beforeEach(function() { page.get('creditApplication'); });

  it('provides navigation to instance search by type', function () {
    page.toInstanceSearchByType();
    expect(searchPage.isDisplayed()).toBeTruthy();
  });

  it('selecting node in graph hi-lights node', function() {
    var nodeId = 'decisionEngine';
    expect(page.graph.isSelected(nodeId)).toBeFalsy();
    page.graph.select(nodeId);
    expect(page.graph.isSelected(nodeId)).toBeTruthy();
  });

  describe('tabs', function() {
    describe('active', function() {
      function assertActiveTab(isActiveInstancesActive, isAllInstancesActive, isWorkflowSettingsActive,
                               isRadiatorActive) {
        expect(page.tabs.isActiveInstancesActive()).toBe(isActiveInstancesActive);
        expect(page.tabs.isAllInstancesActive()).toBe(isAllInstancesActive);
        expect(page.tabs.isWorkflowSettingsActive()).toBe(isWorkflowSettingsActive);
        expect(page.tabs.isRadiatorActive()).toBe(isRadiatorActive);
      }

      it('all instances is active by default, rest can be activated by clicking', function () {
        assertActiveTab(true, false, false, false);

        page.tabs.activateAllInstances();
        assertActiveTab(false, true, false, false);

        page.tabs.activateWorkflowSettings();
        assertActiveTab(false, false, true, false);

        page.tabs.activateRadiator();
        assertActiveTab(false, false, false, true);

        page.tabs.activateActiveInstances();
        assertActiveTab(true, false, false, false);
      });
    });

    xdescribe('all instances', function() {
      beforeEach(function () {
        page.tabs.activateAllInstances();
      });

      it('clicking instance hi-lights corresponding node in graph', function() {});

      it('provides navigation to instance search by type and state', function() {});
    });

    xdescribe('workflow settings', function() {
      beforeEach(function () {
        page.tabs.activeWorkflowSettings();
      });

    });

    xdescribe('radiator', function() {
      beforeEach(function () {
        page.tabs.activeRadiator();
      });

    });
  });

});
