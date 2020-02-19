package io.nflow.rest.v1.converter;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

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
    ofNullable(request.archiveWorkflows).ifPresent(config -> convert(config, builder.withArchiveWorkflows()));
    ofNullable(request.deleteArchivedWorkflows).ifPresent(config -> convert(config, builder.withDeleteArchivedWorkflows()));
    ofNullable(request.deleteWorkflows).ifPresent(config -> convert(config, builder.withDeleteWorkflows()));
    return builder.build();
  }

  private void convert(MaintenanceRequestItem config, MaintenanceConfiguration.ConfigurationItem.Builder builder) {
    builder
        .setOlderThanPeriod(config.olderThanPeriod) //
        .setBatchSize(config.batchSize) //
        .setWorkflowTypes(ofNullable(config.workflowTypes).orElse(emptySet())) //
        .done();
  }

  public MaintenanceResponse convert(MaintenanceResults results) {
    MaintenanceResponse response = new MaintenanceResponse();
    response.archivedWorkflows = results.archivedWorkflows;
    response.deletedArchivedWorkflows = results.deletedArchivedWorkflows;
    response.deletedWorkflows = results.deletedWorkflows;
    return response;
  }

}
