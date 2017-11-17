package io.nflow.rest.v1.jaxrs;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.CONFLICT;

import java.net.URI;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.config.jaxrs.NflowCors;
import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.converter.CreateWorkflowConverter;
import io.nflow.rest.v1.converter.ListWorkflowInstanceConverter;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.SetSignalRequest;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/nflow/v1/workflow-instance")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Api("nFlow workflow instance management")
@Component
@NflowCors
public class WorkflowInstanceResource extends ResourceBase {
  private final WorkflowInstanceService workflowInstances;
  private final CreateWorkflowConverter createWorkflowConverter;
  private final ListWorkflowInstanceConverter listWorkflowConverter;
  private final WorkflowInstanceFactory workflowInstanceFactory;

  @Inject
  public WorkflowInstanceResource(WorkflowInstanceService workflowInstances, CreateWorkflowConverter createWorkflowConverter,
      ListWorkflowInstanceConverter listWorkflowConverter, WorkflowInstanceFactory workflowInstanceFactory) {
    this.workflowInstances = workflowInstances;
    this.createWorkflowConverter = createWorkflowConverter;
    this.listWorkflowConverter = listWorkflowConverter;
    this.workflowInstanceFactory = workflowInstanceFactory;
  }

  @OPTIONS
  @Path("{any: .*}")
  @ApiOperation(value = "CORS preflight handling")
  @Consumes(WILDCARD)
  public Response corsPreflight() {
    return Response.ok().build();
  }

  @PUT
  @ApiOperation(value = "Submit new workflow instance")
  @ApiResponses(@ApiResponse(code = 201, message = "Workflow was created", response = CreateWorkflowInstanceResponse.class))
  public Response createWorkflowInstance(
      @Valid @ApiParam(value = "Submitted workflow instance information", required = true) CreateWorkflowInstanceRequest req) {
    WorkflowInstance instance = createWorkflowConverter.convert(req);
    final int id = workflowInstances.insertWorkflowInstance(instance);
    instance = workflowInstances.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null);
    return Response.created(URI.create(String.valueOf(id))).entity(createWorkflowConverter.convert(instance)).build();
  }

  @PUT
  @Path("/{id}")
  @ApiOperation(value = "Update workflow instance", notes = "The service is typically used in manual state "
      + "transition via nFlow Explorer or a business UI.")
  @ApiResponses({ @ApiResponse(code = 204, message = "If update was successful"),
      @ApiResponse(code = 409, message = "If workflow was executing and no update was done") })
  public Response updateWorkflowInstance(@ApiParam("Internal id for workflow instance") @PathParam("id") int id,
      @ApiParam("Submitted workflow instance information") UpdateWorkflowInstanceRequest req) {
    final boolean updated = super.updateWorkflowInstance(id, req, workflowInstanceFactory, workflowInstances);
    return (updated ? noContent() : status(CONFLICT)).build();
  }

  @GET
  @Path("/{id}")
  @ApiOperation(value = "Fetch a workflow instance", notes = "Fetch full state and action history of a single workflow instance.")
  public ListWorkflowInstanceResponse fetchWorkflowInstance(
      @ApiParam("Internal id for workflow instance") @PathParam("id") int id,
      @QueryParam("include") @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @QueryParam("maxActions") @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    try {
      return super.fetchWorkflowInstance(id, include, maxActions,
          this.workflowInstances, this.listWorkflowConverter);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException(format("Workflow instance %s not found", id));
    }
  }

  @GET
  @ApiOperation(value = "List workflow instances", response = ListWorkflowInstanceResponse.class, responseContainer = "List")
  public Collection<ListWorkflowInstanceResponse> listWorkflowInstances(
      @QueryParam("id") @ApiParam("Internal id of workflow instance") List<Integer> ids,
      @QueryParam("type") @ApiParam("Workflow definition type of workflow instance") List<String> types,
      @QueryParam("parentWorkflowId") @ApiParam("Id of parent workflow instance") Integer parentWorkflowId,
      @QueryParam("parentActionId") @ApiParam("Id of parent workflow instance action") Integer parentActionId,
      @QueryParam("state") @ApiParam("Current state of workflow instance") List<String> states,
      @QueryParam("status") @ApiParam("Current status of workflow instance") List<WorkflowInstanceStatus> statuses,
      @QueryParam("businessKey") @ApiParam("Business key for workflow instance") String businessKey,
      @QueryParam("externalId") @ApiParam("External id for workflow instance") String externalId,
      @QueryParam("include") @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @QueryParam("maxResults") @ApiParam("Maximum number of workflow instances to be returned") Long maxResults,
      @QueryParam("maxActions") @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    return super.listWorkflowInstances(ids, types, parentWorkflowId, parentActionId, states,
        statuses, businessKey, externalId, include, maxResults, maxActions,
        this.workflowInstances, this.listWorkflowConverter);
  }

  @PUT
  @Path("/{id}/signal")
  @ApiOperation(value = "Set workflow instance signal value", notes = "The service may be used for example to interrupt executing workflow instance.")
  @ApiResponses({ @ApiResponse(code = 200, message = "When operation was successful") })
  public Response setSignal(@ApiParam("Internal id for workflow instance") @PathParam("id") int id,
      @Valid @ApiParam("New signal value") SetSignalRequest req) {
    final boolean updated = workflowInstances.setSignal(id, ofNullable(req.signal), req.reason, WorkflowActionType.externalChange);
    return (updated ? ok("Signal was set successfully") : ok("Signal was not set")).build();
  }

}
