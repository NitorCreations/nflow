package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_MAINTENANCE_PATH;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_MAINTENANCE_TAG;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.nflow.engine.service.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceResults;
import io.nflow.engine.service.MaintenanceService;
import io.nflow.rest.config.springweb.SchedulerService;
import io.nflow.rest.v1.converter.MaintenanceConverter;
import io.nflow.rest.v1.msg.MaintenanceRequest;
import io.nflow.rest.v1.msg.MaintenanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_MAINTENANCE_PATH, produces = APPLICATION_JSON_VALUE)
@Component
@Tag(name = NFLOW_MAINTENANCE_TAG)
public class MaintenanceResource extends SpringWebResource {

  private final MaintenanceService maintenanceService;
  private final MaintenanceConverter converter;

  @Inject
  public MaintenanceResource(SchedulerService scheduler, MaintenanceService maintenanceService, MaintenanceConverter converter) {
    super(scheduler);
    this.maintenanceService = maintenanceService;
    this.converter = converter;
  }

  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  @Operation(description = "Do maintenance on old workflow instances synchronously")
  @ApiResponse(responseCode = "200", description = "Maintenance operation status",
      content = @Content(schema = @Schema(implementation = MaintenanceResponse.class)))
  public Mono<ResponseEntity<?>> cleanupWorkflows(
      @RequestBody @Valid @Parameter(description = "Parameters for the maintenance process",
          required = true) MaintenanceRequest request) {
    return handleExceptions(() -> wrapBlocking(() -> {
      MaintenanceConfiguration configuration = converter.convert(request);
      MaintenanceResults results = maintenanceService.cleanupWorkflows(configuration);
      return ok(converter.convert(results));
    }));
  }
}
