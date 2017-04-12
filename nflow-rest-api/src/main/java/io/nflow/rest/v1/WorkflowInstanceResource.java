package io.nflow.rest.v1;

import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.joda.time.DateTime.now;
import static org.springframework.util.StringUtils.isEmpty;

import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

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

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.config.NflowCors;
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
public class WorkflowInstanceResource {
  private final WorkflowInstanceService workflowInstances;
  private final CreateWorkflowConverter createWorkflowConverter;
  private final ListWorkflowInstanceConverter listWorkflowConverter;
  private final WorkflowInstanceFactory workflowInstanceFactory;
  private static final String currentStateVariables = "currentStateVariables";
  private static final String actions = "actions";
  private static final String actionStateVariables = "actionStateVariables";
  private static final String childWorkflows = "childWorkflows";
  private static final Map<String, WorkflowInstanceInclude> INCLUDE_STRING_TO_ENUM = unmodifiableMap(Stream
      .of(new SimpleEntry<>(currentStateVariables, WorkflowInstanceInclude.CURRENT_STATE_VARIABLES),
          new SimpleEntry<>(actions, WorkflowInstanceInclude.ACTIONS),
          new SimpleEntry<>(actionStateVariables, WorkflowInstanceInclude.ACTION_STATE_VARIABLES),
          new SimpleEntry<>(childWorkflows, WorkflowInstanceInclude.CHILD_WORKFLOW_IDS))
      .collect(toMap(Entry::getKey, Entry::getValue)));
  private static final String INCLUDE_PARAM_VALUES = currentStateVariables + "," + actions + "," + actionStateVariables + ","
      + childWorkflows;
  private static final String INCLUDE_PARAM_DESC = "Data to include in response. " + currentStateVariables
      + " = current stateVariables for worfklow, " + actions + " = state transitions, " + actionStateVariables
      + " = state variable changes for actions, " + childWorkflows + " = map of created child workflow instance IDs by action ID";

  @Inject
  public WorkflowInstanceResource(WorkflowInstanceService workflowInstances, CreateWorkflowConverter createWorkflowConverter,
      ListWorkflowInstanceConverter listWorkflowConverter, WorkflowInstanceFactory workflowInstanceFactory) {
    this.workflowInstances = workflowInstances;
    this.createWorkflowConverter = createWorkflowConverter;
    this.listWorkflowConverter = listWorkflowConverter;
    this.workflowInstanceFactory = workflowInstanceFactory;
  }

  @PUT
  @ApiOperation(value = "Submit new workflow instance")
  @ApiResponses(@ApiResponse(code = 201, message = "Workflow was created", response = CreateWorkflowInstanceResponse.class))
  public Response createWorkflowInstance(
      @Valid @ApiParam(value = "Submitted workflow instance information", required = true) CreateWorkflowInstanceRequest req) {
    WorkflowInstance instance = createWorkflowConverter.convert(req);
    int id = workflowInstances.insertWorkflowInstance(instance);
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
    WorkflowInstance.Builder builder = workflowInstanceFactory.newWorkflowInstanceBuilder().setId(id)
        .setNextActivation(req.nextActivationTime);
    String msg = defaultIfBlank(req.actionDescription, "");
    if (!isEmpty(req.state)) {
      builder.setState(req.state);
      if (isBlank(req.actionDescription)) {
        msg = "API changed state to " + req.state + ". ";
      }
    }
    if (req.nextActivationTime != null && isBlank(req.actionDescription)) {
      msg += "API changed nextActivationTime to " + req.nextActivationTime + ". ";
    }
    if (!req.stateVariables.isEmpty()) {
      for (Entry<String, Object> entry : req.stateVariables.entrySet()) {
        Object value = entry.getValue();
        if (value instanceof String) {
          builder.putStateVariable(entry.getKey(), (String) value);
        } else {
          builder.putStateVariable(entry.getKey(), value);
        }
      }
      if (isBlank(req.actionDescription)) {
        msg += "API updated state variables. ";
      }
    }
    if (msg.isEmpty()) {
      return noContent().build();
    }
    WorkflowInstance instance = builder.setStateText(msg).build();
    WorkflowInstanceAction action = new WorkflowInstanceAction.Builder(instance).setType(externalChange)
        .setStateText(trimToNull(msg)).setExecutionEnd(now()).build();
    boolean updated = workflowInstances.updateWorkflowInstance(instance, action);
    return (updated ? noContent() : status(CONFLICT)).build();
  }

  @GET
  @Path("/{id}")
  @ApiOperation(value = "Fetch a workflow instance", notes = "Fetch full state and action history of a single workflow instance.")
  public ListWorkflowInstanceResponse fetchWorkflowInstance(
      @ApiParam("Internal id for workflow instance") @PathParam("id") int id,
      @QueryParam("include") @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @QueryParam("maxActions") @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    Set<WorkflowInstanceInclude> includes = parseIncludeEnums(include);
    // TODO: move to include parameters in next major version
    includes.add(WorkflowInstanceInclude.STARTED);
    try {
      WorkflowInstance instance = workflowInstances.getWorkflowInstance(id, includes, maxActions);
      return listWorkflowConverter.convert(instance, includes);
    } catch (@SuppressWarnings("unused") EmptyResultDataAccessException e) {
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
    Set<String> includeStrings = parseIncludeStrings(include).collect(toSet());
    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder() //
        .addIds(ids.toArray(new Integer[ids.size()])) //
        .addTypes(types.toArray(new String[types.size()])) //
        .setParentWorkflowId(parentWorkflowId) //
        .setParentActionId(parentActionId) //
        .addStates(states.toArray(new String[states.size()])) //
        .addStatuses(statuses.toArray(new WorkflowInstanceStatus[statuses.size()])) //
        .setBusinessKey(businessKey) //
        .setExternalId(externalId) //
        .setIncludeCurrentStateVariables(includeStrings.contains(currentStateVariables)) //
        .setIncludeActions(includeStrings.contains(actions)) //
        .setIncludeActionStateVariables(includeStrings.contains(actionStateVariables)) //
        .setMaxResults(maxResults) //
        .setMaxActions(maxActions) //
        .setIncludeChildWorkflows(includeStrings.contains(childWorkflows)).build();
    Collection<WorkflowInstance> instances = workflowInstances.listWorkflowInstances(q);
    List<ListWorkflowInstanceResponse> resp = new ArrayList<>();
    Set<WorkflowInstanceInclude> parseIncludeEnums = parseIncludeEnums(include);
    // TODO: move to include parameters in next major version
    parseIncludeEnums.add(WorkflowInstanceInclude.STARTED);
    for (WorkflowInstance instance : instances) {
      resp.add(listWorkflowConverter.convert(instance, parseIncludeEnums));
    }
    return resp;
  }

  private Set<WorkflowInstanceInclude> parseIncludeEnums(String include) {
    return parseIncludeStrings(include).map(INCLUDE_STRING_TO_ENUM::get).filter(Objects::nonNull)
        .collect(toCollection(HashSet::new));
  }

  private Stream<String> parseIncludeStrings(String include) {
    return Stream.of(trimToEmpty(include).split(","));
  }

  @PUT
  @Path("/{id}/signal")
  @ApiOperation(value = "Set workflow instance signal value", notes = "The service may be used for example to interrupt executing workflow instance.")
  @ApiResponses({ @ApiResponse(code = 200, message = "When operation was successful") })
  public Response setSignal(@ApiParam("Internal id for workflow instance") @PathParam("id") int id,
      @Valid @ApiParam("New signal value") SetSignalRequest req) {
    boolean updated = workflowInstances.setSignal(id, ofNullable(req.signal), req.reason, WorkflowActionType.externalChange);
    return (updated ? ok("Signal was set successfully") : ok("Signal was not set")).build();
  }

}
