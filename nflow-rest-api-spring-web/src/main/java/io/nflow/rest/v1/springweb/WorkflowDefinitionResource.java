package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_DEFINITION_PATH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

import java.util.List;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.rest.config.springweb.SchedulerService;
import io.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_WORKFLOW_DEFINITION_PATH, produces = APPLICATION_JSON_VALUE)
@Api("nFlow workflow definition management")
@Component
public class WorkflowDefinitionResource extends SpringWebResource {

  private final WorkflowDefinitionService workflowDefinitions;
  private final ListWorkflowDefinitionConverter converter;
  private final WorkflowDefinitionDao workflowDefinitionDao;

  @Inject
  public WorkflowDefinitionResource(SchedulerService scheduler, WorkflowDefinitionService workflowDefinitions,
      ListWorkflowDefinitionConverter converter, WorkflowDefinitionDao workflowDefinitionDao) {
    super(scheduler);
    this.workflowDefinitions = workflowDefinitions;
    this.converter = converter;
    this.workflowDefinitionDao = workflowDefinitionDao;
  }

  @GetMapping
  @ApiOperation(value = "List workflow definitions", response = ListWorkflowDefinitionResponse.class, responseContainer = "List", notes = "Returns workflow definition(s): all possible states, transitions between states and other setting metadata. The workflow definition can deployed in nFlow engine or historical workflow definition stored in the database.")
  public Mono<ResponseEntity<?>> listWorkflowDefinitions(
      @RequestParam(value = "type", defaultValue = "") @ApiParam("Included workflow types") List<String> types) {
    return handleExceptions(() -> wrapBlocking(
        () -> ok(super.listWorkflowDefinitions(types, workflowDefinitions, converter, workflowDefinitionDao))));
  }
}
