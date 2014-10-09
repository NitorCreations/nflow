package com.nitorcreations.nflow.rest.v1;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.WorkflowExecutorService;
import com.nitorcreations.nflow.engine.workflow.executor.WorkflowExecutor;
import com.nitorcreations.nflow.rest.v1.converter.ListWorkflowExecutorConverter;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowExecutorResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/v1/workflow-executor")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "/workflow-executor", description = "Query workflow executors")
@Component
public class WorkflowExecutorResource {

  private final WorkflowExecutorService workflowExecutors;
  private final ListWorkflowExecutorConverter converter;

  @Inject
  public WorkflowExecutorResource(WorkflowExecutorService workflowExecutors, ListWorkflowExecutorConverter converter) {
    this.workflowExecutors = workflowExecutors;
    this.converter = converter;
  }

  @GET
  @ApiOperation(value = "List workflow executors", response = ListWorkflowExecutorResponse.class, responseContainer = "List")
  public Collection<ListWorkflowExecutorResponse> listWorkflowExecutors() {
    List<WorkflowExecutor> executors = workflowExecutors.getWorkflowExecutors();
    Collection<ListWorkflowExecutorResponse> response = new ArrayList<>();
    for (WorkflowExecutor executor : executors) {
      response.add(converter.convert(executor));
    }
    return response;
  }
}
