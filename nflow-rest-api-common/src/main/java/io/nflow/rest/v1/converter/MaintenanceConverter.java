package io.nflow.rest.v1.converter;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

import java.util.function.Supplier;

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
    apply(request.archiveWorkflows, builder::withArchiveWorkflows);
    apply(request.deleteArchivedWorkflows, builder::withDeleteArchivedWorkflows);
    apply(request.deleteWorkflows, builder::withDeleteWorkflows);
    return builder.build();
  }

  private void apply(MaintenanceRequestItem requestItem, Supplier<ConfigurationItem.Builder> builderSupplier) {
    ofNullable(requestItem).ifPresent(item -> builderSupplier.get() //
            .setOlderThanPeriod(item.olderThanPeriod) //
            .setBatchSize(item.batchSize) //
            .setWorkflowTypes(ofNullable(item.workflowTypes).orElse(emptySet())) //
            .done());
  }

  public MaintenanceResponse convert(MaintenanceResults results) {
    MaintenanceResponse response = new MaintenanceResponse();
    response.archivedWorkflows = results.archivedWorkflows;
    response.deletedArchivedWorkflows = results.deletedArchivedWorkflows;
    response.deletedWorkflows = results.deletedWorkflows;
    return response;
  }

}
