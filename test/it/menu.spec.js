'use strict';

var po = require('./pageobjects/pageobjects');

describe('menu', function () {
  var menu = po.menu({});
  var frontPage = po.frontPage({});

  function assertTabs(isDefinitionsTabActive, isInstancesTabActive, isAboutTabActive) {
    expect(menu.isDefinitionsActive()).toBe(isDefinitionsTabActive);
    expect(menu.isInstancesActive()).toBe(isInstancesTabActive);
    expect(menu.isAboutActive()).toBe(isAboutTabActive);
  }

  function assertDefinitionsTabActive() {
    assertTabs(true, false, false);
  }

  function assertInstancesTabActive() {
    assertTabs(false, true, false);
  }

  function assertAboutTabActive() {
    assertTabs(false, false, true);
  }

  beforeEach(function () { frontPage.get(); });

  it('click based tab navigation', function() {
    assertDefinitionsTabActive();

    menu.toInstances();
    assertInstancesTabActive();

    menu.toAbout();
    assertAboutTabActive();

    menu.toDefinitions();
    assertDefinitionsTabActive();
  });
});
