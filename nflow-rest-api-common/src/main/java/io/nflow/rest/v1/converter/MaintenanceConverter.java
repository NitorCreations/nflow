package io.nflow.rest.v1.converter;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;
import io.nflow.engine.service.MaintenanceResults;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceRequest.MaintenanceRequestItem;
import io.nflow.rest.v1.msg.MaintenanceResponse;

@Component
public class MaintenanceConverter {

  public MaintenanceConfiguration convert(MaintenanceRequest request) {
    MaintenanceConfiguration.Builder builder = new MaintenanceConfiguration.Builder();
    if (request.archiveWorkflows != null) {
      builder.setArchiveWorkflows(createConfig(request.archiveWorkflows));
    }
    if (request.deleteArchivedWorkflows != null) {
      builder.setDeleteArchivedWorkflows(createConfig(request.deleteArchivedWorkflows));
    }
    if (request.deleteWorkflows != null) {
      builder.setDeleteWorkflows(createConfig(request.deleteWorkflows));
    }
    return builder.build();
  }

  private ConfigurationItem createConfig(MaintenanceRequestItem config) {
    return new ConfigurationItem.Builder().setOlderThanPeriod(config.olderThanPeriod).setBatchSize(config.batchSize).build();
  }

  public MaintenanceResponse convert(MaintenanceResults results) {
    MaintenanceResponse response = new MaintenanceResponse();
    response.archivedWorkflows = results.archivedWorkflows;
    response.deletedArchivedWorkflows = results.deletedArchivedWorkflows;
    response.deletedWorkflows = results.deletedWorkflows;
    return response;
  }

}
