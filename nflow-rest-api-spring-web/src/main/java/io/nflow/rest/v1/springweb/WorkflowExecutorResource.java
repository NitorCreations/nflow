package io.nflow.rest.v1.springweb;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_EXECUTOR_PATH;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.rest.v1.converter.ListWorkflowExecutorConverter;
import io.nflow.rest.v1.msg.ListWorkflowExecutorResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping(value = NFLOW_WORKFLOW_EXECUTOR_PATH, produces = APPLICATION_JSON_VALUE)
@Api("nFlow workflow executor management")
@Component
public class WorkflowExecutorResource {

  private final WorkflowExecutorService workflowExecutors;
  private final ListWorkflowExecutorConverter converter;

  @Autowired
  public WorkflowExecutorResource(WorkflowExecutorService workflowExecutors, ListWorkflowExecutorConverter converter) {
    this.workflowExecutors = workflowExecutors;
    this.converter = converter;
  }

  @GetMapping
  @ApiOperation(value = "List workflow executors", response = ListWorkflowExecutorResponse.class, responseContainer = "List")
  public Collection<ListWorkflowExecutorResponse> listWorkflowExecutors() {
    return workflowExecutors.getWorkflowExecutors().stream().map(executor -> converter.convert(executor)).collect(toList());
  }
}
