package com.nitorcreations.nflow.rest.v1;

import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.service.ArchiveService;
import com.nitorcreations.nflow.rest.v1.msg.ArchiveRequest;
import com.nitorcreations.nflow.rest.v1.msg.ArchiveResponse;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveResourceTest {

  @InjectMocks
  private final ArchiveResource resource = new ArchiveResource();
  @Mock
  private ArchiveService service;
  @Mock
  private Response expected;

  DateTime olderThan = now().minusYears(1);
  int batchSize = 10;
  int archived = 100;

  @Test
  public void archiveDelegatesToArchiveService() {
    when(service.archiveWorkflows(olderThan, batchSize)).thenReturn(archived);

    ArchiveRequest request = new ArchiveRequest();
    request.olderThan = olderThan;
    request.batchSize = batchSize;
    ArchiveResponse response = resource.archiveWorkflows(request);

    verify(service).archiveWorkflows(olderThan, batchSize);
    assertThat(response.archivedWorkflows, is(archived));
  }
}
