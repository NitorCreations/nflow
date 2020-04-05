package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_MAINTENANCE_PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceResults;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.converter.MaintenanceConverter;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@Path(NFLOW_MAINTENANCE_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@OpenAPIDefinition(info = @Info(
        title = "nFlow maintenance"
))
@Component
@NflowCors
public class MaintenanceResource extends JaxRsResource {

  @Inject
  private MaintenanceService maintenanceService;

  @Inject
  private MaintenanceConverter converter;

  @POST
  @Operation(description = "Do maintenance on old workflow instances synchronously")
  public MaintenanceResponse cleanupWorkflows(
      @ApiParam(value = "Parameters for the maintenance process", required = true) MaintenanceRequest request) {
    return handleExceptions(() -> {
      MaintenanceConfiguration configuration = converter.convert(request);
      MaintenanceResults results = maintenanceService.cleanupWorkflows(configuration);
      return ok(converter.convert(results));
    });
  }
}
