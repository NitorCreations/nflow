package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_MAINTENANCE_PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceService;
import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration.Builder;
import io.nflow.engine.service.MaintenanceService.MaintenanceResults;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path(NFLOW_MAINTENANCE_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api("nFlow maintenance")
@Component
@NflowCors
public class MaintenanceResource {

  @Inject
  private MaintenanceService archiveService;

  @POST
  @ApiOperation("Do maintenance on old workflow instances synchronously")
  public MaintenanceResponse cleanupWorkflows(
      @ApiParam(value = "Parameters for the maintenance process", required = true) MaintenanceRequest request) {
    MaintenanceConfiguration configuration = new Builder()
        .setBatchSize(request.batchSize)
        .setDeleteArchivedWorkflowsOlderThan(request.deleteArchivedWorkflowsOlderThan)
        .setArchiveWorkflowsOlderThan(request.archiveWorkflowsOlderThan)
        .setDeleteStatesOlderThan(request.deleteWorkflowsOlderThan)
        .build();
    MaintenanceResults results = archiveService.cleanupWorkflows(configuration);
    MaintenanceResponse response = new MaintenanceResponse();
    response.deletedArchivedWorkflows = results.deletedArchivedWorkflows;
    response.archivedWorkflows = results.archivedWorkflows;
    response.deletedWorkflows = results.deletedWorkflows;
    return response;
  }
}
