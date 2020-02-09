package io.nflow.rest.v1.converter;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration.Builder;
import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.MaintenanceService.MaintenanceResults;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceRequest.MaintenanceRequestItem;
import io.nflow.rest.v1.msg.MaintenanceResponse;

@Component
public class MaintenanceConverter {

  public MaintenanceConfiguration convert(MaintenanceRequest request) {
    Builder builder = new MaintenanceConfiguration.Builder();
    if (request.archiveWorkflows != null) {
      builder.setArchiveWorkflows(createConfig(request.archiveWorkflows));
    }
    if (request.deleteArchivedWorkflows != null) {
      builder.setArchiveWorkflows(createConfig(request.deleteArchivedWorkflows));
    }
    if (request.deleteWorkflows != null) {
      builder.setArchiveWorkflows(createConfig(request.deleteWorkflows));
    }
    return builder.build();
  }

  private ConfigurationItem createConfig(MaintenanceRequestItem config) {
    return new ConfigurationItem.Builder().setOlderThanPeriod(config.olderThanPeriod).setBatchSize(config.batchSize).build();
  }

  public MaintenanceResponse convert(MaintenanceResults results) {
    MaintenanceResponse response = new MaintenanceResponse();
    response.deletedArchivedWorkflows = results.deletedArchivedWorkflows;
    response.archivedWorkflows = results.archivedWorkflows;
    response.deletedWorkflows = results.deletedWorkflows;
    return response;
  }

}
