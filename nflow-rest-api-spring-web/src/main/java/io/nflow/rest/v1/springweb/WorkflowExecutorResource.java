package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_EXECUTOR_PATH;
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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_WORKFLOW_EXECUTOR_PATH, produces = APPLICATION_JSON_VALUE)
@Api("nFlow workflow executor management")
@Component
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
  @ApiOperation(value = "List workflow executors", response = ListWorkflowExecutorResponse.class, responseContainer = "List")
  public Mono<ResponseEntity<?>> listWorkflowExecutors() {
    return handleExceptions(() -> wrapBlocking(
        () -> ok(workflowExecutors.getWorkflowExecutors().stream().map(converter::convert).collect(toList()))));
  }
}
