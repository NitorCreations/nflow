'use strict';

function assertHeader(expected) {
  var e = element.all(by.tagName('h2')).first();
  expect(e.getText()).toEqual(expected);
}

describe('nflow-ui homepage', function() {

  var mainPageLink = element(by.linkText('Workflow definitions'));
  var searchPageLink = element(by.linkText('Workflow instances'));
  var aboutPageLink = element(by.linkText('About'));

  beforeEach(function() {
    browser.get('http://localhost:9001');
  });

  it('has navi', function() {
    assertHeader('Workflow definitions');

    searchPageLink.click();
    assertHeader('Search workflow instances');

    aboutPageLink.click();
    assertHeader('nFlow Explorer');

    mainPageLink.click();
    assertHeader('Workflow definitions');
  });

  it('has list of workflow definitions', function() {
    expect(element(by.linkText('creditDecision')).isDisplayed()).toBeTruthy();
    expect(element(by.linkText('processCreditApplication')).isDisplayed()).toBeTruthy();
    expect(element(by.linkText('withdrawLoan')).isDisplayed()).toBeTruthy();
  });

  it('provides navigation to workflow definitions', function() {
    element(by.linkText('creditDecision')).click();
    assertHeader('creditDecision');

    element(by.linkText('Search related workflow instances')).click();
    assertHeader('Search workflow instances');
  });
});

describe('workflow definition page', function() {
  beforeEach(function() {
    browser.get('http://localhost:9000/#/workflow-definition/creditDecision');
  });

  it('provides navigation to instance search', function () {
    element(by.linkText('Search related workflow instances')).click();
    assertHeader('Search workflow instances');
  });
});
