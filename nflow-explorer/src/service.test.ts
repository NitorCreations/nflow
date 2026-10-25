import {getWorkflowInstance, listChildWorkflowInstances} from './service';

const config: any = {
  activeNflowEndpoint: {apiUrl: 'http://nflow.test/api'}
};

const mockFetch = (response: Partial<Response>) => {
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
};

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('service', () => {
  it('getWorkflowInstance queries the archive', async () => {
    const fetchMock = mockFetch({ok: true, json: async () => ({id: 1})});
    await getWorkflowInstance(config, 1);
    expect(fetchMock.mock.calls[0][0]).toContain('queryArchive=true');
  });

  it('getWorkflowInstance rejects when the instance is not found', async () => {
    mockFetch({ok: false, status: 404, json: async () => ({})});
    await expect(getWorkflowInstance(config, 1)).rejects.toThrow('404');
  });

  it('listChildWorkflowInstances queries the archive', async () => {
    const fetchMock = mockFetch({ok: true, json: async () => []});
    await listChildWorkflowInstances(config, 1);
    expect(fetchMock.mock.calls[0][0]).toContain('parentWorkflowId=1');
    expect(fetchMock.mock.calls[0][0]).toContain('queryArchive=true');
  });
});
