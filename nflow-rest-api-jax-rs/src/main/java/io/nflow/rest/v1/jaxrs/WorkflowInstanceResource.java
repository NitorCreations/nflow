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
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Path(NFLOW_WORKFLOW_INSTANCE_PATH)
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@OpenAPIDefinition(info = @Info(title = "nFlow workflow instance management"))
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
  @Operation(summary = "CORS preflight handling")
  @Consumes(WILDCARD)
  public Response corsPreflight() {
    return ok().build();
  }

  @PUT
  @Operation(summary = "Submit new workflow instance")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Workflow was created",
          content = @Content(schema = @Schema(implementation = CreateWorkflowInstanceResponse.class))),
      @ApiResponse(responseCode = "400",
          description = "If instance could not be created, for example when state variable value was too long") })
  public Response createWorkflowInstance(
      @Valid @RequestBody(description = "Submitted workflow instance information",
          required = true) CreateWorkflowInstanceRequest req) {
    return handleExceptions(() -> {
      WorkflowInstance instance = createWorkflowConverter.convert(req);
      long id = workflowInstances.insertWorkflowInstance(instance);
      instance = workflowInstances.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null);
      return created(URI.create(String.valueOf(id))).entity(createWorkflowConverter.convert(instance));
    });
  }

  @PUT
  @Path("/id/{id}")
  @Operation(summary = "Update workflow instance", description = "The service is typically used in manual state "
      + "transition via nFlow Explorer or a business UI.")
  @ApiResponses({ @ApiResponse(responseCode = "204", description = "If update was successful"),
      @ApiResponse(responseCode = "400",
          description = "If instance could not be updated, for example when state variable value was too long"),
      @ApiResponse(responseCode = "409", description = "If workflow was executing and no update was done") })
  public Response updateWorkflowInstance(@Parameter(description = "Internal id for workflow instance") @PathParam("id") long id,
      @Valid @RequestBody(description = "Submitted workflow instance information") UpdateWorkflowInstanceRequest req) {
    return handleExceptions(() -> {
      boolean updated = super.updateWorkflowInstance(id, req, workflowInstanceFactory, workflowInstances, workflowInstanceDao);
      return (updated ? noContent() : status(CONFLICT));
    });
  }

  @GET
  @Path("/id/{id}")
  @Operation(summary = "Fetch a workflow instance",
      description = "Fetch full state and action history of a single workflow instance.")
  @SuppressFBWarnings(value = "LEST_LOST_EXCEPTION_STACK_TRACE",
      justification = "The empty result exception contains no useful information")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "",
          content = @Content(schema = @Schema(implementation = ListWorkflowInstanceResponse.class))),
      @ApiResponse(responseCode = "404",
          description = "If instance could not be created, for example when state variable value was too long") })
  public Response fetchWorkflowInstance(@Parameter(description = "Internal id for workflow instance") @PathParam("id") long id,
      @QueryParam("include") @Parameter(description = INCLUDE_PARAM_DESC/*
                                                                         * , allowableValues = INCLUDE_PARAM_VALUES, allowMultiple
                                                                         * = true
                                                                         */) String include,
      @QueryParam("maxActions") @Parameter(
          description = "Maximum number of actions returned for each workflow instance") Long maxActions,
      @QueryParam("queryArchive") @Parameter(
          description = "Query also the archive if not found from main tables") Boolean queryArchive) {
    return handleExceptions(() -> ok(super.fetchWorkflowInstance(id, include, maxActions,
        ofNullable(queryArchive).orElse(QUERY_ARCHIVED_DEFAULT), workflowInstances, listWorkflowConverter)));
  }

  @GET
  @Operation(summary = "List workflow instances")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListWorkflowInstanceResponse.class))))
  public Response listWorkflowInstances(
      @QueryParam("id") @Parameter(description = "Internal id of workflow instance") List<Long> ids,
      @QueryParam("type") @Parameter(description = "Workflow definition type of workflow instance") List<String> types,
      @QueryParam("parentWorkflowId") @Parameter(description = "Id of parent workflow instance") Long parentWorkflowId,
      @QueryParam("parentActionId") @Parameter(description = "Id of parent workflow instance action") Long parentActionId,
      @QueryParam("state") @Parameter(description = "Current state of workflow instance") List<String> states,
      @QueryParam("status") @Parameter(description = "Current status of workflow instance") List<WorkflowInstanceStatus> statuses,
      @QueryParam("businessKey") @Parameter(description = "Business key for workflow instance") String businessKey,
      @QueryParam("externalId") @Parameter(description = "External id for workflow instance") String externalId,
      @QueryParam("stateVariableKey") @Parameter(
          description = "Key of state variable that must exist for workflow instance") String stateVariableKey,
      @QueryParam("stateVariableValue") @Parameter(
          description = "Current value of state variable defined by stateVariableKey") String stateVariableValue,
      @QueryParam("include") @Parameter(description = INCLUDE_PARAM_DESC
      /* , allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true */) String include,
      @QueryParam("maxResults") @Parameter(description = "Maximum number of workflow instances to be returned") Long maxResults,
      @QueryParam("maxActions") @Parameter(
          description = "Maximum number of actions returned for each workflow instance") Long maxActions,
      @QueryParam("queryArchive") @Parameter(
          description = "Query also the archive if not enough results found from main tables") Boolean queryArchive) {
    return handleExceptions(() -> ok(super.listWorkflowInstances(ids, types, parentWorkflowId, parentActionId, states, statuses,
        businessKey, externalId, stateVariableKey, stateVariableValue, include, maxResults, maxActions,
        ofNullable(queryArchive).orElse(QUERY_ARCHIVED_DEFAULT), workflowInstances, listWorkflowConverter).iterator()));
  }

  @PUT
  @Path("/{id}/signal")
  @Operation(summary = "Set workflow instance signal value",
      description = "The service may be used for example to interrupt executing workflow instance.")
  @ApiResponse(responseCode = "200", description = "When operation was successful",
      content = @Content(schema = @Schema(implementation = SetSignalResponse.class)))
  public Response setSignal(@Parameter(description = "Internal id for workflow instance") @PathParam("id") long id,
      @Valid @RequestBody(description = "New signal value") SetSignalRequest req) {
    return handleExceptions(() -> {
      SetSignalResponse response = new SetSignalResponse();
      response.setSignalSuccess = workflowInstances.setSignal(id, ofNullable(req.signal), req.reason,
          WorkflowActionType.externalChange);
      return ok(response);
    });
  }

  @PUT
  @Path("/{id}/wakeup")
  @Operation(
      description = "Wake up sleeping workflow instance. If expected states are given, only wake up if the instance is in one of the expected states.")
  @ApiResponse(responseCode = "200", description = "When workflow wakeup was attempted",
      content = @Content(schema = @Schema(implementation = WakeupResponse.class)))
  public Response wakeup(@Parameter(description = "Internal id for workflow instance") @PathParam("id") long id,
      @Valid @RequestBody(description = "Expected states") WakeupRequest req) {
    return handleExceptions(() -> {
      WakeupResponse response = new WakeupResponse();
      List<String> expectedStates = ofNullable(req.expectedStates).orElseGet(Collections::emptyList);
      response.wakeupSuccess = workflowInstances.wakeupWorkflowInstance(id, expectedStates);
      return ok(response);
    });
  }
}
