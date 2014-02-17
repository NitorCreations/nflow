package com.nitorcreations.nflow.rest.v0;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nitorcreations.nflow.engine.domain.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.rest.v0.converter.CreateWorkflowConverter;
import com.nitorcreations.nflow.rest.v0.converter.ListWorkflowConverter;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/workflow-instance")
@Produces("application/json")
@Api(value = "/workflow-instance", description = "Manage workflow instances")
public class WorkflowInstanceResource {
  private RepositoryService repositoryService;
  private CreateWorkflowConverter createWorkflowConverter;
  private ListWorkflowConverter listWorkflowConverter;

  @Inject
  public void setRepositoryService(
      RepositoryService repositoryService, CreateWorkflowConverter createWorkflowConverter, ListWorkflowConverter listWorkflowConverter) {
    this.repositoryService = repositoryService;
    this.createWorkflowConverter = createWorkflowConverter;
    this.listWorkflowConverter = listWorkflowConverter;
  }

  @PUT
  @ApiOperation("Submit new workflow instance")
  public CreateWorkflowInstanceResponse createWorkflowInstance(
      @Valid CreateWorkflowInstanceRequest req) throws JsonProcessingException {
    WorkflowInstance instance = createWorkflowConverter.convertAndValidate(req);
    int id = repositoryService.insertWorkflowInstance(instance);
    return createWorkflowConverter.convert(id, instance);
  }

  @GET
  @ApiOperation("List workflow instances")
  public Collection<ListWorkflowInstanceResponse> listWorkflowInstances(
      @QueryParam("type") String[] types, @QueryParam("state") String[] states) throws JsonProcessingException {
    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().addTypes(types).addStates(states).build();
    Collection<WorkflowInstance> instances = repositoryService.listWorkflowInstances(q);
    List<ListWorkflowInstanceResponse> resp = new ArrayList<>();
    for (WorkflowInstance instance : instances) {
      resp.add(listWorkflowConverter.convert(instance));
    }
    return resp;
  }

}
