package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_STATISTICS_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.nflow.engine.service.StatisticsService;
import io.nflow.rest.v1.converter.StatisticsConverter;
import io.nflow.rest.v1.msg.StatisticsResponse;
import io.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_STATISTICS_PATH, produces = APPLICATION_JSON_VALUE)
@Api("nFlow statistics")
@Component
public class StatisticsResource {

  @Inject
  private StatisticsService statisticsService;
  @Inject
  private StatisticsConverter statisticsConverter;

  @GetMapping
  @ApiOperation(value = "Get executor group statistics", notes = "Returns counts of queued and executing workflow instances.")
  public StatisticsResponse queryStatistics() {
    return statisticsConverter.convert(statisticsService.getStatistics());
  }

  @GetMapping(path="/workflow/{type}")
  @ApiOperation("Get workflow definition statistics")
  public WorkflowDefinitionStatisticsResponse getStatistics(
      @PathVariable("type") @ApiParam(value = "Workflow definition type", required = true) String type,
      @RequestParam(value = "createdAfter", required = false) @ApiParam("Include only workflow instances created after given time") DateTime createdAfter,
      @RequestParam(value = "createdBefore", required = false) @ApiParam("Include only workflow instances created before given time") DateTime createdBefore,
      @RequestParam(value = "modifiedAfter", required = false) @ApiParam("Include only workflow instances modified after given time") DateTime modifiedAfter,
      @RequestParam(value = "modifiedBefore", required = false) @ApiParam("Include only workflow instances modified before given time") DateTime modifiedBefore) {
    return statisticsConverter.convert(
        statisticsService.getWorkflowDefinitionStatistics(type, createdAfter, createdBefore, modifiedAfter,
        modifiedBefore));
  }
}
