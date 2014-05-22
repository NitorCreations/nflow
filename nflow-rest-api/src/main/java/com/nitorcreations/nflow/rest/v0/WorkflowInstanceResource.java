package com.nitorcreations.nflow.rest.v0;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.joda.time.DateTime.now;
import static org.springframework.util.StringUtils.isEmpty;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nitorcreations.nflow.engine.domain.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.service.RepositoryService;
import com.nitorcreations.nflow.rest.v0.converter.CreateWorkflowConverter;
import com.nitorcreations.nflow.rest.v0.converter.ListWorkflowInstanceConverter;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v0.msg.UpdateWorkflowInstanceRequest;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

@Path("/v0/workflow-instance")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "/workflow-instance", description = "Manage workflow instances")
@Component
public class WorkflowInstanceResource {

  private final RepositoryService repositoryService;
  private final CreateWorkflowConverter createWorkflowConverter;
  private final ListWorkflowInstanceConverter listWorkflowConverter;

  @Inject
  public WorkflowInstanceResource(
      RepositoryService repositoryService, CreateWorkflowConverter createWorkflowConverter, ListWorkflowInstanceConverter listWorkflowConverter) {
    this.repositoryService = repositoryService;
    this.createWorkflowConverter = createWorkflowConverter;
    this.listWorkflowConverter = listWorkflowConverter;
  }

  @PUT
  @ApiOperation(value = "Submit new workflow instance", response = CreateWorkflowInstanceResponse.class)
  public Response createWorkflowInstance(@Valid CreateWorkflowInstanceRequest req) throws JsonProcessingException {
    WorkflowInstance instance = createWorkflowConverter.convertAndValidate(req);
    int id = repositoryService.insertWorkflowInstance(instance);
    if (id == -1) {
      QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().addTypes(req.type).setExternalId(req.externalId).build();
      instance = repositoryService.listWorkflowInstances(query).iterator().next();
      id = instance.id;
    } else {
      instance = repositoryService.getWorkflowInstance(id);
    }
    return Response.created(URI.create(String.valueOf(id))).entity(createWorkflowConverter.convert(instance)).build();
  }

  @PUT
  @Path("/{id}")
  @ApiOperation(value = "Update workflow instance state")
  public void updateWorkflowInstance(
      @ApiParam("Internal id for workflow instance")
      @PathParam("id") int id,
      UpdateWorkflowInstanceRequest req) throws JsonProcessingException {
    // TODO: requires more work, e.g. concurrent check with engine, validation
    WorkflowInstance instance = repositoryService.getWorkflowInstance(id);
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance);
    if (!isEmpty(req.state)) {
      builder.setState(req.state);
      builder.setRetries(0);
    }
    if (req.nextActivationTime != null) {
      builder.setNextActivation(req.nextActivationTime);
    }
    repositoryService.updateWorkflowInstance(builder.build(), new WorkflowInstanceAction.Builder(instance).setExecutionStart(instance.modified)
        .setExecutionEnd(now()).build());
  }

  @GET
  @ApiOperation(value = "List workflow instances", response = ListWorkflowInstanceResponse.class, responseContainer = "List")
  public Collection<ListWorkflowInstanceResponse> listWorkflowInstances(
      @QueryParam("type")
      @ApiParam(value = "Type of workflow instance")
      String[] types,
      @QueryParam("state")
      @ApiParam(value = "Current state of workflow instance")
      String[] states,
      @QueryParam("businessKey")
      @ApiParam(value = "Business key for workflow instance")
      String businessKey,
      @QueryParam("externalId")
      @ApiParam(value = "External id for workflow instance")
      String externalId,
      @QueryParam("include")
      @ApiParam(value = "Data to include in response. actions = state transitions.", allowableValues="actions")
      String include) throws JsonProcessingException {
    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().addTypes(types).addStates(states).setBusinessKey(businessKey)
        .setExternalId(externalId).setIncludeActions("actions".equals(include)).build();
    Collection<WorkflowInstance> instances = repositoryService.listWorkflowInstances(q);
    List<ListWorkflowInstanceResponse> resp = new ArrayList<>();
    for (WorkflowInstance instance : instances) {
      resp.add(listWorkflowConverter.convert(instance, q));
    }
    return resp;
  }

}
