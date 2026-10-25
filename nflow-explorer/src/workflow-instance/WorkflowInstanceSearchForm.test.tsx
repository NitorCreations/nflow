import React from 'react';
import {renderToStaticMarkup} from 'react-dom/server';
import {MemoryRouter} from 'react-router-dom';
import WorkflowInstanceSearchForm from './WorkflowInstanceSearchForm';

vi.mock('../config', () => ({
  ConfigContext: React.createContext({}),
  readConfig: () => Promise.resolve({}),
  useConfig: () => ({})
}));

const render = (search: string) =>
  renderToStaticMarkup(
    <MemoryRouter initialEntries={['/workflow' + search]}>
      <WorkflowInstanceSearchForm definitions={[]} onSubmit={() => {}} />
    </MemoryRouter>
  );

const archiveCheckbox = (html: string) =>
  html.match(/<input[^>]*type="checkbox"[^>]*>/)?.[0] || '';

describe('WorkflowInstanceSearchForm', () => {
  it('does not include archived instances by default', () => {
    const html = render('');
    expect(html).toContain('Include archived instances');
    expect(archiveCheckbox(html)).not.toContain('checked');
  });

  it('restores the archive checkbox from the queryArchive URL parameter', () => {
    expect(archiveCheckbox(render('?queryArchive=true'))).toContain('checked');
  });
});
