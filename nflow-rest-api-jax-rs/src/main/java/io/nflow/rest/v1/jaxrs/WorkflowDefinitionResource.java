package io.nflow.rest.v1.jaxrs;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.springframework.stereotype.Component;

import io.nflow.engine.internal.dao.WorkflowDefinitionDao;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.converter.ListWorkflowDefinitionConverter;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/nflow/v1/workflow-definition")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api("nFlow workflow definition management")
@Component
@NflowCors
public class WorkflowDefinitionResource extends ResourceBase {

  private final WorkflowDefinitionService workflowDefinitions;
  private final ListWorkflowDefinitionConverter converter;
  private final WorkflowDefinitionDao workflowDefinitionDao;

  @Inject
  public WorkflowDefinitionResource(WorkflowDefinitionService workflowDefinitions, ListWorkflowDefinitionConverter converter,
      WorkflowDefinitionDao workflowDefinitionDao) {
    this.workflowDefinitions = workflowDefinitions;
    this.converter = converter;
    this.workflowDefinitionDao = workflowDefinitionDao;
  }

  @GET
  @ApiOperation(value = "List workflow definitions", response = ListWorkflowDefinitionResponse.class, responseContainer = "List",
    notes = "Returns workflow definition(s): all possible states, transitions between states and other setting metadata."
      + "The workflow definition can deployed in nFlow engine or historical workflow definition stored in the database.")
  public List<ListWorkflowDefinitionResponse> listWorkflowDefinitions(
      @QueryParam("type") @ApiParam(value = "Included workflow types") List<String> types) {
    return super.listWorkflowDefinitions(types, this.workflowDefinitions, this.converter, this.workflowDefinitionDao);
  }
}
