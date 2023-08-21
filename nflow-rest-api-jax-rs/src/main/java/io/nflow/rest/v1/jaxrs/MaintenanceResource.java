package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_MAINTENANCE_PATH;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_MAINTENANCE_TAG;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.ok;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceResults;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.converter.MaintenanceConverter;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path(NFLOW_MAINTENANCE_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Component
@NflowCors
@Tag(name = NFLOW_MAINTENANCE_TAG)
public class MaintenanceResource extends JaxRsResource {

  @Inject
  private MaintenanceService maintenanceService;

  @Inject
  private MaintenanceConverter converter;

  @POST
  @Operation(summary = "Execute workflow instance maintenance", description = "Runs requested maintenance tasks synchronously")
  @ApiResponse(responseCode = "200", description = "Maintenance operation status",
      content = @Content(schema = @Schema(implementation = MaintenanceResponse.class)))
  public Response cleanupWorkflows(
      @Valid @RequestBody(description = "Parameters for the maintenance process", required = true) MaintenanceRequest request) {
    return handleExceptions(() -> {
      MaintenanceConfiguration configuration = converter.convert(request);
      MaintenanceResults results = maintenanceService.cleanupWorkflows(configuration);
      return ok(converter.convert(results));
    });
  }
}
