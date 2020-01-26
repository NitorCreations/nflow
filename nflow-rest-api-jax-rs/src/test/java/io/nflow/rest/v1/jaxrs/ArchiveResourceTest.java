package io.nflow.rest.v1.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import io.nflow.engine.service.ArchiveService;
import io.nflow.rest.v1.msg.ArchiveRequest;
import io.nflow.rest.v1.msg.ArchiveResponse;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
  @SuppressWarnings("deprecation")
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
