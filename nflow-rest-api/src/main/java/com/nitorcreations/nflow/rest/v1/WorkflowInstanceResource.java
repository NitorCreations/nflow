package com.nitorcreations.nflow.rest.v1;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
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
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.WorkflowInstanceService;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.rest.v1.converter.CreateWorkflowConverter;
import com.nitorcreations.nflow.rest.v1.converter.ListWorkflowInstanceConverter;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path("/v1/workflow-instance")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api(value = "/workflow-instance", description = "Manage workflow instances")
@Component
public class WorkflowInstanceResource {
  private final WorkflowInstanceService workflowInstances;
  private final CreateWorkflowConverter createWorkflowConverter;
  private final ListWorkflowInstanceConverter listWorkflowConverter;
  public static final String currentStateVariables = "currentStateVariables";
  public static final String actions = "actions";
  public static final String actionStateVariables = "actionStateVariables";
  @Inject
  public WorkflowInstanceResource(
      WorkflowInstanceService workflowInstances, CreateWorkflowConverter createWorkflowConverter, ListWorkflowInstanceConverter listWorkflowConverter) {
    this.workflowInstances = workflowInstances;
    this.createWorkflowConverter = createWorkflowConverter;
    this.listWorkflowConverter = listWorkflowConverter;
  }

  @PUT
  @ApiOperation(value = "Submit new workflow instance", response = CreateWorkflowInstanceResponse.class)
  @ApiResponse(code = 201, message = "Workflow was created")
  public Response createWorkflowInstance(@Valid CreateWorkflowInstanceRequest req) {
    WorkflowInstance instance = createWorkflowConverter.convertAndValidate(req);
    int id = workflowInstances.insertWorkflowInstance(instance);
    instance = workflowInstances.getWorkflowInstance(id);
    return Response.created(URI.create(String.valueOf(id))).entity(createWorkflowConverter.convert(instance)).build();
  }

  @PUT
  @Path("/{id}")
  @ApiOperation(value = "Update workflow instance state")
  @ApiResponses({ @ApiResponse(code = 204, message = "If update was sucessful"),
      @ApiResponse(code = 409, message = "If workflow was executing and no update was done") })
  public Response updateWorkflowInstance(
      @ApiParam("Internal id for workflow instance")
      @PathParam("id") int id,
      UpdateWorkflowInstanceRequest req) {
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder().setId(id);
    String msg = defaultIfBlank(req.actionDescription, "");
    if (!isEmpty(req.state)) {
      builder.setState(req.state);
      if (isBlank(req.actionDescription)) {
        msg = "API changed state to " + req.state + ". ";
      }
    }
    if (req.nextActivationTime != null) {
      builder.setNextActivation(req.nextActivationTime);
      if (isBlank(req.actionDescription)) {
        msg += "API changed nextActivationTime to " + req.nextActivationTime + ".";
      }
    }
    if (msg.isEmpty()) {
      return noContent().build();
    }
    WorkflowInstance instance = builder.build();
    boolean updated = workflowInstances.updateWorkflowInstance(instance, new WorkflowInstanceAction.Builder(instance)
        .setStateText(trimToNull(msg)).setExecutionEnd(now()).build());
    return (updated ? noContent() : status(CONFLICT)).build();
  }

  @GET
  @Path("/{id}")
  @ApiOperation(value = "Fetch a workflow instance", response = ListWorkflowInstanceResponse.class)
  public ListWorkflowInstanceResponse fetchWorkflowInstance(
      @ApiParam("Internal id for workflow instance")
      @PathParam("id") int id) {
    Collection<ListWorkflowInstanceResponse> instances = listWorkflowInstances(new Integer[] { id }, new String[0],
        new String[0], null, null, actions + "," + currentStateVariables + "," + actionStateVariables, 1L);
    if(instances.isEmpty()) {
      throw new NotFoundException(format("Workflow instance %s not found", id));
    }
    return instances.iterator().next();
  }

  @GET
  @ApiOperation(value = "List workflow instances", response = ListWorkflowInstanceResponse.class, responseContainer = "List")
  public Collection<ListWorkflowInstanceResponse> listWorkflowInstances(
      @QueryParam("id")
      @ApiParam(value = "Internal id of workflow instance")
      Integer[] ids,
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
      @ApiParam(value = "Data to include in response. currentStateVariables = current stateVariables for worfklow, actions = state transitions, actionStateVariables = state variable changes for actions",
        allowableValues = currentStateVariables + "," + actions + "," + actionStateVariables,
        allowMultiple = true)
      String include,
      @QueryParam("maxResults")
      @ApiParam(value = "Maximum number of workflow instances to be returned")
      Long maxResults) {
    List<String> includes = parseIncludes(include);
    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().addIds(ids).addTypes(types).addStates(states)
        .setBusinessKey(businessKey).setExternalId(externalId)
        .setIncludeCurrentStateVariables(includes.contains(currentStateVariables)).setIncludeActions(includes.contains(actions))
        .setIncludeActionStateVariables(includes.contains(actionStateVariables))
        .setMaxResults(maxResults).build();
    Collection<WorkflowInstance> instances = workflowInstances.listWorkflowInstances(q);
    List<ListWorkflowInstanceResponse> resp = new ArrayList<>();
    for (WorkflowInstance instance : instances) {
      resp.add(listWorkflowConverter.convert(instance, q));
    }
    return resp;
  }

  private List<String> parseIncludes(String include) {
    return asList(trimToEmpty(include).split(","));
  }

}
