package com.nitorcreations.nflow.rest.v1;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.StatisticsService;
import com.nitorcreations.nflow.rest.v1.converter.StatisticsConverter;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/v1/statistics")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "/statistics", description = "Query general statistics")
@Component
public class StatisticsResource {

  @Inject
  private StatisticsService statisticsService;
  @Inject
  private StatisticsConverter statisticsConverter;

  @GET
  @ApiOperation(value = "Query statistics", response = StatisticsResponse.class)
  public StatisticsResponse queryStatistics() {
    return statisticsConverter.convert(statisticsService.queryStatistics());
  }

  @GET
  @Path("/{type}/statistics")
  @ApiOperation(value = "Get workflow definition statistics", response = WorkflowDefinitionStatisticsResponse.class)
  public WorkflowDefinitionStatisticsResponse getStatistics(@PathParam("type") String type,
      @QueryParam("createdAfter") DateTime createdAfter, @QueryParam("createdBefore") DateTime createdBefore,
      @QueryParam("modifiedAfter") DateTime modifiedAfter, @QueryParam("modifiedBefore") DateTime modifiedBefore) {
    return statisticsConverter.convert(statisticsService.getWorkflowDefinitionStatistics(type, createdAfter, createdBefore, modifiedAfter,
        modifiedBefore));
  }
}
