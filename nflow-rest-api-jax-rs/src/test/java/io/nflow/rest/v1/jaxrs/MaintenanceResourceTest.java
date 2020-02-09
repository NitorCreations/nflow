package io.nflow.rest.v1.jaxrs;

import static org.joda.time.DateTime.now;

import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.service.MaintenanceService;

@ExtendWith(MockitoExtension.class)
public class MaintenanceResourceTest {

  @InjectMocks
  private final MaintenanceResource resource = new MaintenanceResource();
  @Mock
  private MaintenanceService service;
  @Mock
  private Response expected;

  DateTime olderThan = now().minusYears(1);
  int batchSize = 10;
  int archived = 100;

  // @Test
  // @SuppressWarnings("deprecation")
  // public void archiveDelegatesToArchiveService() {
  // when(service.archiveWorkflows(olderThan, batchSize)).thenReturn(archived);
  //
  // ArchiveRequest request = new ArchiveRequest();
  // request.olderThan = olderThan;
  // request.batchSize = batchSize;
  // ArchiveResponse response = resource.archiveWorkflows(request);
  //
  // verify(service).archiveWorkflows(olderThan, batchSize);
  // assertThat(response.archivedWorkflows, is(archived));
  // }
}
