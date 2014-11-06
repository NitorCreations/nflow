var _ = require('lodash');

describe('nflow-ui homepage', function() {
  var history = element.all(by.repeater('result in memory'));

  var mainPageLink = element(by.linkText('Workflow definitions'));
  var searchPageLink = element(by.linkText('Workflow instances'));
  var aboutPageLink = element(by.linkText('About'));

  beforeEach(function() {
    browser.get('http://localhost:9001');
  });

  function assertHeader(expected) {
    var e = element.all(by.tagName('h2')).first();
    expect(e.getText()).toEqual(expected);
  };

  it('should have navi', function() {
    assertHeader('Workflows');

    searchPageLink.click();
    assertHeader('Search workflows');

    aboutPageLink.click();
    assertHeader('nFlow UI');

    mainPageLink.click();
    assertHeader('Workflows');
  });

  it('should have list of workflow definitions', function() {
    var link = element(by.linkText('creditDecision'));
    link.click();
    assertHeader('creditDecision');

    var radiatorLink = element(by.linkText('Open radiator'));
    radiatorLink.click();
    assertHeader('Radiator creditDecision');

    browser.navigate().back();
    assertHeader('creditDecision');

    var searchLink = element(by.linkText('Search related workflow instances'));
    searchLink.click();
    assertHeader('Search workflows');
  });
});

describe('workflow definition page', function() {




});
