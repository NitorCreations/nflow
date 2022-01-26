package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_EXECUTOR_PATH;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_EXECUTOR_TAG;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.rest.config.springweb.SchedulerService;
import io.nflow.rest.v1.converter.ListWorkflowExecutorConverter;
import io.nflow.rest.v1.msg.ListWorkflowExecutorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_WORKFLOW_EXECUTOR_PATH, produces = APPLICATION_JSON_VALUE)
@Component
@Tag(name = NFLOW_WORKFLOW_EXECUTOR_TAG)
public class WorkflowExecutorResource extends SpringWebResource {

  private final WorkflowExecutorService workflowExecutors;
  private final ListWorkflowExecutorConverter converter;

  @Inject
  public WorkflowExecutorResource(SchedulerService scheduler, WorkflowExecutorService workflowExecutors,
      ListWorkflowExecutorConverter converter) {
    super(scheduler);
    this.workflowExecutors = workflowExecutors;
    this.converter = converter;
  }

  @GetMapping
  @Operation(summary = "List workflow executors")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListWorkflowExecutorResponse.class))))
  public Mono<ResponseEntity<?>> listWorkflowExecutors() {
    return handleExceptions(() -> wrapBlocking(
        () -> ok(workflowExecutors.getWorkflowExecutors().stream().map(converter::convert).collect(toList()))));
  }
}
