var _ = require('lodash');

describe('angularjs homepage', function() {
  var history = element.all(by.repeater('result in memory'));

  var mainPageLink = element(by.linkText('Workflow definitions'));
  var searchPageLink = element(by.linkText('Workflow instances'));
  var aboutPageLink = element(by.linkText('About'));

  beforeEach(function() {
    browser.get('http://localhost:9001');
  });

  function assertHeader(expected) {
    var e = element(by.tagName('h2'));
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

});
