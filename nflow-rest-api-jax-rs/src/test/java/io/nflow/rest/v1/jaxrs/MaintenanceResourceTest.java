package io.nflow.rest.v1.jaxrs;

import static org.joda.time.Period.years;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.joda.time.ReadablePeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceService.MaintenanceResults;
import io.nflow.rest.v1.converter.MaintenanceConverter;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceResponse;

@ExtendWith(MockitoExtension.class)
public class MaintenanceResourceTest {

  @InjectMocks
  private final MaintenanceResource resource = new MaintenanceResource();
  @Mock
  private MaintenanceService service;
  @Spy
  private final MaintenanceConverter converter = new MaintenanceConverter();
  @Captor
  private ArgumentCaptor<MaintenanceConfiguration> configCaptor;

  @Test
  public void archiveDelegatesToArchiveService() {
    int batchSize = 10;
    ReadablePeriod period = years(1);
    MaintenanceResults maintenanceResults = new MaintenanceResults();
    maintenanceResults.archivedWorkflows = 10;
    maintenanceResults.deletedArchivedWorkflows = 20;
    maintenanceResults.deletedWorkflows = 30;
    when(service.cleanupWorkflows(any(MaintenanceConfiguration.class))).thenReturn(maintenanceResults);

    MaintenanceRequest request = new MaintenanceRequest();
    request.archiveWorkflowsOlderThan = period;
    request.batchSize = batchSize;
    MaintenanceResponse response = resource.cleanupWorkflows(request);

    verify(service).cleanupWorkflows(configCaptor.capture());
    MaintenanceConfiguration configuration = configCaptor.getValue();
    assertEquals(batchSize, configuration.batchSize);
    assertEquals(period, configuration.archiveWorkflowsOlderThan);
    assertEquals(maintenanceResults.archivedWorkflows, response.archivedWorkflows);
    assertEquals(maintenanceResults.deletedArchivedWorkflows, response.deletedArchivedWorkflows);
    assertEquals(maintenanceResults.deletedWorkflows, response.deletedWorkflows);
  }
}
