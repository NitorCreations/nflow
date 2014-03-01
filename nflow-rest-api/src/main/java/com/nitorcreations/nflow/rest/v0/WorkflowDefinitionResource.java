package com.nitorcreations.nflow.rest.v0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.rest.v0.converter.ListWorkflowDefinitionConverter;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowDefinitionResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/v0/workflow-definition")
@Produces("application/json")
@Api(value = "/workflow-definition", description = "Query installed workflow definitions")
@Component
public class WorkflowDefinitionResource {

  private final RepositoryService repositoryService;
  private final ListWorkflowDefinitionConverter converter;

  @Inject
  public WorkflowDefinitionResource(RepositoryService repositoryService, ListWorkflowDefinitionConverter converter) {
    this.repositoryService = repositoryService;
    this.converter = converter;
  }

  @GET
  @ApiOperation(value = "List workflow definitions", response = ListWorkflowDefinitionResponse.class, responseContainer = "List")
  public Collection<ListWorkflowDefinitionResponse> listWorkflowInstances() throws JsonProcessingException {
    List<WorkflowDefinition<? extends WorkflowState>> definitions = repositoryService.getWorkflowDefinitions();
    Collection<ListWorkflowDefinitionResponse> response = new ArrayList<>();
    for (WorkflowDefinition<? extends WorkflowState> definition : definitions) {
      response.add(converter.convert(definition));
    }
    return response;
  }

}
