package io.nflow.rest.v1.converter;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceService.MaintenanceResults;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceResponse;

@Component
public class MaintenanceConverter {

  public MaintenanceConfiguration convert(MaintenanceRequest request) {
    return new MaintenanceConfiguration.Builder() //
        .setBatchSize(request.batchSize) //
        .setDeleteArchivedWorkflowsOlderThan(request.deleteArchivedWorkflowsOlderThan) //
        .setArchiveWorkflowsOlderThan(request.archiveWorkflowsOlderThan) //
        .setDeleteStatesOlderThan(request.deleteWorkflowsOlderThan) //
        .build();
  }

  public MaintenanceResponse convert(MaintenanceResults results) {
    MaintenanceResponse response = new MaintenanceResponse();
    response.deletedArchivedWorkflows = results.deletedArchivedWorkflows;
    response.archivedWorkflows = results.archivedWorkflows;
    response.deletedWorkflows = results.deletedWorkflows;
    return response;
  }

}
