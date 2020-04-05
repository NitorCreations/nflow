package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_STATISTICS_PATH;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.ok;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Parameter;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import io.nflow.engine.service.StatisticsService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.converter.StatisticsConverter;
import io.nflow.rest.v1.msg.StatisticsResponse;
import io.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

@Path(NFLOW_STATISTICS_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@OpenAPIDefinition(info = @Info(
        title = "nFlow statistics"
))
@Component
@NflowCors
public class StatisticsResource extends JaxRsResource {

  @Inject
  private StatisticsService statisticsService;
  @Inject
  private StatisticsConverter statisticsConverter;

  @GET
  @Operation(summary = "Get executor group statistics", description = "Returns counts of queued and executing workflow instances.")
  public StatisticsResponse queryStatistics() {
    return handleExceptions(() -> ok(statisticsConverter.convert(statisticsService.getStatistics())));
  }

  @GET
  @Path("/workflow/{type}")
  @Operation(summary = "Get workflow definition statistics")
  public WorkflowDefinitionStatisticsResponse getStatistics(@PathParam("type") @ApiParam(value = "Workflow definition type", required = true) String type,
      @QueryParam("createdAfter") @ApiParam(value = "Include only workflow instances created after given time") DateTime createdAfter,
      @QueryParam("createdBefore") @ApiParam("Include only workflow instances created before given time") DateTime createdBefore,
      @QueryParam("modifiedAfter") @ApiParam("Include only workflow instances modified after given time") DateTime modifiedAfter,
      @QueryParam("modifiedBefore") @ApiParam("Include only workflow instances modified before given time") DateTime modifiedBefore) {
    return handleExceptions(() -> ok(statisticsConverter.convert(
        statisticsService.getWorkflowDefinitionStatistics(type, createdAfter, createdBefore, modifiedAfter, modifiedBefore))));
  }
}
