'use strict';

var po = require('./pageobjects/pageobjects');

describe('front page', function() {

  var frontPage = po.frontPage({});
  var workflowDefinitionPage = po.workflowDefinitionPage({});

  beforeEach(function() { frontPage.get(); });

  it('has list of workflow definitions', function() {
    // TODO to page object
    expect(element(by.linkText('creditDecision')).isDisplayed()).toBeTruthy();
    expect(element(by.linkText('processCreditApplication')).isDisplayed()).toBeTruthy();
    expect(element(by.linkText('withdrawLoan')).isDisplayed()).toBeTruthy();
  });

  it('provides navigation to workflow definitions', function() {
    // TODO to page object
    element(by.linkText('creditDecision')).click();

    expect(workflowDefinitionPage.isDisplayed()).toBeTruthy();
  });
});
