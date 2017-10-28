package io.nflow.rest.v1.jaxrs;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.Collection;
import static java.util.stream.Collectors.toList;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import io.nflow.engine.service.WorkflowExecutorService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.converter.ListWorkflowExecutorConverter;
import io.nflow.rest.v1.msg.ListWorkflowExecutorResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/nflow/v1/workflow-executor")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api("nFlow workflow executor management")
@Component
@NflowCors
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
    return workflowExecutors.getWorkflowExecutors().stream().map(executor -> converter.convert(executor)).collect(toList());
  }
}
