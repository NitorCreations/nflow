import React from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
import {MemoryRouter} from 'react-router-dom';
import {ReturnLink} from './ReturnLink';

const render = (returnUrl: string, label = 'Return to application') =>
  renderToStaticMarkup(
    <MemoryRouter
      initialEntries={[
        '/workflow?returnUrl=' +
          encodeURIComponent(returnUrl) +
          '&returnUrlLabel=' +
          encodeURIComponent(label)
      ]}
    >
      <ReturnLink />
    </MemoryRouter>
  );

describe('ReturnLink', () => {
  it('renders an http(s) returnUrl as a link with the given label', () => {
    const html = render(
      'https://orders.example.com/order/123',
      'Back to order'
    );
    expect(html).toContain('href="https://orders.example.com/order/123"');
    expect(html).toContain('>Back to order<');
  });

  it('does not render a javascript: returnUrl into the anchor href', () => {
    // Regression test for reflected XSS: a crafted link could run script in the
    // Explorer's origin as soon as the operator clicked the return link.
    const html = render('javascript:alert(document.domain)', 'Return to order');
    expect(html).not.toContain('javascript:');
    expect(html).not.toContain('<a ');
  });

  it('does not render other non-web schemes either', () => {
    expect(render('data:text/html,hello')).not.toContain('<a ');
    expect(render('vbscript:MsgBox')).not.toContain('<a ');
  });

  it('renders nothing when returnUrl is absent', () => {
    const html = renderToStaticMarkup(
      <MemoryRouter initialEntries={['/workflow']}>
        <ReturnLink />
      </MemoryRouter>
    );
    expect(html).not.toContain('<a ');
  });
});
