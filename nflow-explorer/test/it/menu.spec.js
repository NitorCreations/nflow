'use strict';

var po = require('./pageobjects/pageobjects');

describe('menu', function () {
  var menu = po.menu({});
  var frontPage = po.frontPage({});

  function assertTabs(isDefinitionsTabActive, isInstancesTabActive, isExecutorsTabActive, isAboutTabActive) {
    expect(menu.isDefinitionsActive()).toBe(isDefinitionsTabActive);
    expect(menu.isInstancesActive()).toBe(isInstancesTabActive);
    expect(menu.isExecutorsActive()).toBe(isExecutorsTabActive);
    expect(menu.isAboutActive()).toBe(isAboutTabActive);
    expect(menu.isEnpointSelectionPresent()).toBe(true);
  }

  function assertDefinitionsTabActive() {
    assertTabs(true, false, false, false);
  }

  function assertInstancesTabActive() {
    assertTabs(false, true, false, false);
  }

  function assertExecutorsTabActive() {
    assertTabs(false, false, true, false);
  }

  function assertAboutTabActive() {
    assertTabs(false, false, false, true);
  }

  beforeEach(function () { frontPage.get(); });

  it('click based tab navigation', function() {
    assertDefinitionsTabActive();

    menu.toInstances();
    assertInstancesTabActive();

    menu.toExecutors();
    assertExecutorsTabActive();

    menu.toAbout();
    assertAboutTabActive();

    menu.toDefinitions();
    assertDefinitionsTabActive();
  });
});
