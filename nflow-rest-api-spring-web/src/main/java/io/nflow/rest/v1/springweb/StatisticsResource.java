package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_STATISTICS_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

import javax.inject.Inject;

import io.nflow.rest.v1.msg.WakeupResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.joda.time.DateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.nflow.engine.service.StatisticsService;
import io.nflow.rest.config.springweb.SchedulerService;
import io.nflow.rest.v1.converter.StatisticsConverter;
import io.nflow.rest.v1.msg.StatisticsResponse;
import io.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_STATISTICS_PATH, produces = APPLICATION_JSON_VALUE)
@OpenAPIDefinition(info = @Info(
        title = "nFlow statistics"
))
@Component
public class StatisticsResource extends SpringWebResource {

  private final StatisticsService statisticsService;
  private final StatisticsConverter statisticsConverter;

  @Inject
  public StatisticsResource(SchedulerService scheduler, StatisticsConverter statisticsConverter,
      StatisticsService statisticsService) {
    super(scheduler);
    this.statisticsService = statisticsService;
    this.statisticsConverter = statisticsConverter;
  }

  @GetMapping
  @Operation(summary = "Get executor group statistics", description = "Returns counts of queued and executing workflow instances.")
  @ApiResponse(responseCode = "200", description = "Statistics", content = @Content(schema = @Schema(implementation = StatisticsResponse.class)))
  public Mono<ResponseEntity<?>> queryStatistics() {
    return handleExceptions(() -> wrapBlocking(() -> ok(statisticsConverter.convert(statisticsService.getStatistics()))));
  }

  @GetMapping(path="/workflow/{type}")
  @Operation(summary = "Get workflow definition statistics")
  @ApiResponse(responseCode = "200", description = "Statistics", content = @Content(schema = @Schema(implementation = WorkflowDefinitionStatisticsResponse.class)))
  public Mono<ResponseEntity<?>> getStatistics(
          @PathVariable("type") @Parameter(description = "Workflow definition type", required = true) String type,
          @RequestParam(value = "createdAfter", required = false) @Parameter(description = "Include only workflow instances created after given time") DateTime createdAfter,
          @RequestParam(value = "createdBefore", required = false) @Parameter(description = "Include only workflow instances created before given time") DateTime createdBefore,
          @RequestParam(value = "modifiedAfter", required = false) @Parameter(description = "Include only workflow instances modified after given time") DateTime modifiedAfter,
          @RequestParam(value = "modifiedBefore", required = false) @Parameter(description = "Include only workflow instances modified before given time") DateTime modifiedBefore) {
    return handleExceptions(() -> wrapBlocking(() -> ok(statisticsConverter.convert(
        statisticsService.getWorkflowDefinitionStatistics(type, createdAfter, createdBefore, modifiedAfter, modifiedBefore)))));
  }
}
