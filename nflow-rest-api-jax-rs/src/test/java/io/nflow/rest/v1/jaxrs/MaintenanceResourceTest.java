package io.nflow.rest.v1.jaxrs;

import static org.joda.time.Period.months;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import io.nflow.rest.v1.msg.MaintenanceRequest.MaintenanceRequestItem;
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
  public void cleanupWorkflowsDelegatesToMaintenanceService() {
    int batchSize = 10;
    ReadablePeriod period1 = months(1);
    ReadablePeriod period2 = months(2);
    ReadablePeriod period3 = months(3);
    MaintenanceResults maintenanceResults = new MaintenanceResults();
    maintenanceResults.archivedWorkflows = batchSize;
    maintenanceResults.deletedArchivedWorkflows = batchSize * 2;
    maintenanceResults.deletedWorkflows = batchSize * 3;
    when(service.cleanupWorkflows(any(MaintenanceConfiguration.class))).thenReturn(maintenanceResults);

    MaintenanceRequest request = new MaintenanceRequest();
    request.archiveWorkflows = new MaintenanceRequestItem();
    request.archiveWorkflows.olderThanPeriod = period1;
    request.archiveWorkflows.batchSize = batchSize;
    request.deleteArchivedWorkflows = new MaintenanceRequestItem();
    request.deleteArchivedWorkflows.olderThanPeriod = period2;
    request.deleteArchivedWorkflows.batchSize = batchSize * 2;
    request.deleteWorkflows = new MaintenanceRequestItem();
    request.deleteWorkflows.olderThanPeriod = period3;
    request.deleteWorkflows.batchSize = batchSize * 3;

    MaintenanceResponse response = resource.cleanupWorkflows(request);

    verify(service).cleanupWorkflows(configCaptor.capture());
    MaintenanceConfiguration configuration = configCaptor.getValue();
    assertEquals(period1, configuration.archiveWorkflows.olderThanPeriod);
    assertEquals(batchSize, configuration.archiveWorkflows.batchSize);
    assertEquals(period2, configuration.deleteArchivedWorkflows.olderThanPeriod);
    assertEquals(batchSize * 2, configuration.deleteArchivedWorkflows.batchSize);
    assertEquals(period3, configuration.deleteWorkflows.olderThanPeriod);
    assertEquals(batchSize * 3, configuration.deleteWorkflows.batchSize);
    assertNull(configuration.deleteStates);
    assertEquals(maintenanceResults.archivedWorkflows, response.archivedWorkflows);
    assertEquals(maintenanceResults.deletedArchivedWorkflows, response.deletedArchivedWorkflows);
    assertEquals(maintenanceResults.deletedWorkflows, response.deletedWorkflows);
  }
}
