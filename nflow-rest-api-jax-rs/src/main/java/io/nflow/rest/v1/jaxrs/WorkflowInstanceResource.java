package io.nflow.rest.v1.jaxrs;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_INSTANCE_PATH;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.CONFLICT;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.converter.CreateWorkflowConverter;
import io.nflow.rest.v1.converter.ListWorkflowInstanceConverter;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.SetSignalRequest;
import io.nflow.rest.v1.msg.SetSignalResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.WakeupRequest;
import io.nflow.rest.v1.msg.WakeupResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path(NFLOW_WORKFLOW_INSTANCE_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api("nFlow workflow instance management")
@Component
@NflowCors
public class WorkflowInstanceResource extends JaxRsResource {
  private final WorkflowInstanceService workflowInstances;
  private final CreateWorkflowConverter createWorkflowConverter;
  private final ListWorkflowInstanceConverter listWorkflowConverter;
  private final WorkflowInstanceFactory workflowInstanceFactory;
  private final WorkflowInstanceDao workflowInstanceDao;

  @Inject
  public WorkflowInstanceResource(WorkflowInstanceService workflowInstances, CreateWorkflowConverter createWorkflowConverter,
      ListWorkflowInstanceConverter listWorkflowConverter, WorkflowInstanceFactory workflowInstanceFactory,
      WorkflowInstanceDao workflowInstanceDao) {
    this.workflowInstances = workflowInstances;
    this.createWorkflowConverter = createWorkflowConverter;
    this.listWorkflowConverter = listWorkflowConverter;
    this.workflowInstanceFactory = workflowInstanceFactory;
    this.workflowInstanceDao = workflowInstanceDao;
  }

  @OPTIONS
  @Path("{any: .*}")
  @ApiOperation("CORS preflight handling")
  @Consumes(WILDCARD)
  public Response corsPreflight() {
    return ok().build();
  }

  @PUT
  @ApiOperation("Submit new workflow instance")
  @ApiResponses({ @ApiResponse(code = 201, message = "Workflow was created", response = CreateWorkflowInstanceResponse.class),
    @ApiResponse(code = 400, message = "If instance could not be created, for example when state variable value was too long") })
  public Response createWorkflowInstance(
      @Valid @ApiParam(value = "Submitted workflow instance information", required = true) CreateWorkflowInstanceRequest req) {
    return handleExceptions(() -> {
      WorkflowInstance instance = createWorkflowConverter.convert(req);
      long id = workflowInstances.insertWorkflowInstance(instance);
      instance = workflowInstances.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null);
      return created(URI.create(String.valueOf(id))).entity(createWorkflowConverter.convert(instance));
    });
  }

  @PUT
  @Path("/id/{id}")
  @ApiOperation(value = "Update workflow instance", notes = "The service is typically used in manual state transition via nFlow Explorer or a business UI.")
  @ApiResponses({ @ApiResponse(code = 204, message = "If update was successful"),
    @ApiResponse(code = 400, message = "If instance could not be updated, for example when state variable value was too long"),
    @ApiResponse(code = 409, message = "If workflow was executing and no update was done") })
  public Response updateWorkflowInstance(@ApiParam("Internal id for workflow instance") @PathParam("id") long id,
      @ApiParam("Submitted workflow instance information") UpdateWorkflowInstanceRequest req) {
    return handleExceptions(() -> {
      boolean updated = super.updateWorkflowInstance(id, req, workflowInstanceFactory, workflowInstances, workflowInstanceDao);
      return (updated ? noContent() : status(CONFLICT));
    });
  }

  @GET
  @Path("/id/{id}")
  @ApiOperation(value = "Fetch a workflow instance", notes = "Fetch full state and action history of a single workflow instance.")
  @ApiResponses({ @ApiResponse(code = 200, response = ListWorkflowInstanceResponse.class, message = "If instance was found"),
    @ApiResponse(code = 404, message = "If instance was not found") })
  @SuppressFBWarnings(value = "LEST_LOST_EXCEPTION_STACK_TRACE", justification = "The empty result exception contains no useful information")
  public Response fetchWorkflowInstance(@ApiParam("Internal id for workflow instance") @PathParam("id") long id,
      @QueryParam("include") @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @QueryParam("maxActions") @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    return handleExceptions(
        () -> ok(super.fetchWorkflowInstance(id, include, maxActions, workflowInstances, listWorkflowConverter)));
  }

  @GET
  @ApiOperation(value = "List workflow instances", response = ListWorkflowInstanceResponse.class, responseContainer = "List")
  public Response listWorkflowInstances(@QueryParam("id") @ApiParam("Internal id of workflow instance") List<Long> ids,
      @QueryParam("type") @ApiParam("Workflow definition type of workflow instance") List<String> types,
      @QueryParam("parentWorkflowId") @ApiParam("Id of parent workflow instance") Long parentWorkflowId,
      @QueryParam("parentActionId") @ApiParam("Id of parent workflow instance action") Long parentActionId,
      @QueryParam("state") @ApiParam("Current state of workflow instance") List<String> states,
      @QueryParam("status") @ApiParam("Current status of workflow instance") List<WorkflowInstanceStatus> statuses,
      @QueryParam("businessKey") @ApiParam("Business key for workflow instance") String businessKey,
      @QueryParam("externalId") @ApiParam("External id for workflow instance") String externalId,
      @QueryParam("stateVariableKey") @ApiParam("Key of state variable that must exist for workflow instance") String stateVariableKey,
      @QueryParam("stateVariableValue") @ApiParam("Current value of state variable defined by stateVariableKey") String stateVariableValue,
      @QueryParam("include") @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @QueryParam("maxResults") @ApiParam("Maximum number of workflow instances to be returned") Long maxResults,
      @QueryParam("maxActions") @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    return handleExceptions(() -> ok(super.listWorkflowInstances(ids, types, parentWorkflowId, parentActionId, states, statuses,
        businessKey, externalId, stateVariableKey, stateVariableValue, include, maxResults, maxActions, workflowInstances,
        listWorkflowConverter).iterator()));
  }

  @PUT
  @Path("/{id}/signal")
  @ApiOperation(value = "Set workflow instance signal value", response = SetSignalResponse.class, notes = "The service may be used for example to interrupt executing workflow instance.")
  public Response setSignal(@ApiParam("Internal id for workflow instance") @PathParam("id") long id,
      @Valid @ApiParam("New signal value") SetSignalRequest req) {
    return handleExceptions(() -> {
      SetSignalResponse response = new SetSignalResponse();
      response.setSignalSuccess = workflowInstances.setSignal(id, ofNullable(req.signal), req.reason,
          WorkflowActionType.externalChange);
      return ok(response);
    });
  }

  @PUT
  @Path("/{id}/wakeup")
  @ApiOperation(value = "Wake up sleeping workflow instance", response = WakeupResponse.class, notes = "If expected states are given, only wake up if the instance is in one of the expected states.")
  public Response wakeup(@ApiParam("Internal id for workflow instance") @PathParam("id") long id,
      @Valid @ApiParam("Expected states") WakeupRequest req) {
    return handleExceptions(() -> {
      WakeupResponse response = new WakeupResponse();
      List<String> expectedStates = ofNullable(req.expectedStates).orElseGet(Collections::emptyList);
      response.wakeupSuccess = workflowInstances.wakeupWorkflowInstance(id, expectedStates);
      return ok(response);
    });
  }
}
