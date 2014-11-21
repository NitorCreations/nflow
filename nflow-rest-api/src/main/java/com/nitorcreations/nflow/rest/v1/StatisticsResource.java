package com.nitorcreations.nflow.rest.v1;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.StatisticsService;
import com.nitorcreations.nflow.rest.v1.converter.StatisticsConverter;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/v1/statistics")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "/statistics", description = "Query general statistics")
@Component
public class StatisticsResource {
  Logger logger = LoggerFactory.getLogger(StatisticsResource.class);
  private final StatisticsService statisticsService;
  private final StatisticsConverter statisticsConverter;
  @Inject
  public StatisticsResource(StatisticsService statisticsService, StatisticsConverter statisticsConverter) {
    this.statisticsService = statisticsService;
    this.statisticsConverter = statisticsConverter;
  }

  @GET
  @ApiOperation(value = "Query statistics", response = StatisticsResponse.class)
  public StatisticsResponse queryStatistics() {
    return statisticsConverter.convert(statisticsService.queryStatistics());
  }
}
