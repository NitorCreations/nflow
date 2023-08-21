package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_STATISTICS_PATH;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.ok;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import io.nflow.engine.service.StatisticsService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.ResourcePaths;
import io.nflow.rest.v1.converter.StatisticsConverter;
import io.nflow.rest.v1.msg.StatisticsResponse;
import io.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path(NFLOW_STATISTICS_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Component
@NflowCors
@Tag(name = ResourcePaths.NFLOW_STATISTICS_TAG)
public class StatisticsResource extends JaxRsResource {

  @Inject
  private StatisticsService statisticsService;
  @Inject
  private StatisticsConverter statisticsConverter;

  @GET
  @Operation(summary = "Get executor group statistics",
      description = "Returns counts of queued and executing workflow instances.")
  @ApiResponse(responseCode = "200", description = "Statistics",
      content = @Content(schema = @Schema(implementation = StatisticsResponse.class)))
  public Response queryStatistics() {
    return handleExceptions(() -> ok(statisticsConverter.convert(statisticsService.getStatistics())));
  }

  @GET
  @Path("/workflow/{type}")
  @Operation(summary = "Get workflow definition statistics")
  @ApiResponse(responseCode = "200", description = "Statistics",
      content = @Content(schema = @Schema(implementation = WorkflowDefinitionStatisticsResponse.class)))
  public Response getStatistics(
      @PathParam("type") @Parameter(description = "Workflow definition type", required = true) String type,
      @QueryParam("createdAfter") @Parameter(
          description = "Include only workflow instances created after given time") DateTime createdAfter,
      @QueryParam("createdBefore") @Parameter(
          description = "Include only workflow instances created before given time") DateTime createdBefore,
      @QueryParam("modifiedAfter") @Parameter(
          description = "Include only workflow instances modified after given time") DateTime modifiedAfter,
      @QueryParam("modifiedBefore") @Parameter(
          description = "Include only workflow instances modified before given time") DateTime modifiedBefore) {
    return handleExceptions(() -> ok(statisticsConverter.convert(
        statisticsService.getWorkflowDefinitionStatistics(type, createdAfter, createdBefore, modifiedAfter, modifiedBefore))));
  }
}
