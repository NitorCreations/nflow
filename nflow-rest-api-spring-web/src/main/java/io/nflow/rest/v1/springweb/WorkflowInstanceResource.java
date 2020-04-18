package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_INSTANCE_PATH;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.notFound;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.v1.ResourceBase;
import io.nflow.rest.v1.converter.CreateWorkflowConverter;
import io.nflow.rest.v1.converter.ListWorkflowInstanceConverter;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.SetSignalRequest;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.WakeupRequest;
import io.nflow.rest.v1.msg.WakeupResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_WORKFLOW_INSTANCE_PATH, produces = APPLICATION_JSON_VALUE)
@Api("nFlow workflow instance management")
@Component
public class WorkflowInstanceResource extends ResourceBase {
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

  @PutMapping(consumes = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Submit new workflow instance")
  @ApiResponses({ @ApiResponse(code = 201, message = "Workflow was created", response = CreateWorkflowInstanceResponse.class),
      @ApiResponse(code = 400, message = "If instance could not be created, for example when state variable value was too long") })
  public ResponseEntity<?> createWorkflowInstance(
      @RequestBody @ApiParam(value = "Submitted workflow instance information", required = true) CreateWorkflowInstanceRequest req) {
    WorkflowInstance instance = createWorkflowConverter.convert(req);
    try {
      long id = workflowInstances.insertWorkflowInstance(instance);
      instance = workflowInstances.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null);
      return created(URI.create(String.valueOf(id))).body(createWorkflowConverter.convert(instance));
    } catch (IllegalArgumentException e) {
      return status(BAD_REQUEST).body(e.getMessage());
    }
  }

  @PutMapping(path = "/id/{id}", consumes = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Update workflow instance", notes = "The service is typically used in manual state "
      + "transition via nFlow Explorer or a business UI.")
  @ApiResponses({ @ApiResponse(code = 204, message = "If update was successful"),
      @ApiResponse(code = 400, message = "If instance could not be updated, for example when state variable value was too long"),
      @ApiResponse(code = 409, message = "If workflow was executing and no update was done") })
  public ResponseEntity<?> updateWorkflowInstance(@ApiParam("Internal id for workflow instance") @PathVariable("id") long id,
      @RequestBody @ApiParam("Submitted workflow instance information") UpdateWorkflowInstanceRequest req) {
    try {
      boolean updated = super.updateWorkflowInstance(id, req, workflowInstanceFactory, workflowInstances, workflowInstanceDao);
      return (updated ? noContent() : status(CONFLICT)).build();
    } catch (IllegalArgumentException e) {
      return status(BAD_REQUEST).body(e.getMessage());
    }
  }

  @GetMapping(path = "/id/{id}")
  @ApiOperation(value = "Fetch a workflow instance", notes = "Fetch full state and action history of a single workflow instance.")
  @SuppressFBWarnings(value = "LEST_LOST_EXCEPTION_STACK_TRACE", justification = "The empty result exception contains no useful information")
  public ResponseEntity<ListWorkflowInstanceResponse> fetchWorkflowInstance(
      @ApiParam("Internal id for workflow instance") @PathVariable("id") long id,
      @RequestParam(value = "include", required = false) @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @RequestParam(value = "maxActions", required = false) @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    try {
      return ok().body(super.fetchWorkflowInstance(id, include, maxActions, this.workflowInstances, this.listWorkflowConverter));
    } catch (@SuppressWarnings("unused") EmptyResultDataAccessException e) {
      return notFound().build();
    }
  }

  @GetMapping
  @ApiOperation(value = "List workflow instances", response = ListWorkflowInstanceResponse.class, responseContainer = "List")
  public Iterator<ListWorkflowInstanceResponse> listWorkflowInstances(
      @RequestParam(value = "id", defaultValue = "") @ApiParam("Internal id of workflow instance") List<Long> ids,
      @RequestParam(value = "type", defaultValue = "") @ApiParam("Workflow definition type of workflow instance") List<String> types,
      @RequestParam(value = "parentWorkflowId", required = false) @ApiParam("Id of parent workflow instance") Long parentWorkflowId,
      @RequestParam(value = "parentActionId", required = false) @ApiParam("Id of parent workflow instance action") Long parentActionId,
      @RequestParam(value = "state", defaultValue = "") @ApiParam("Current state of workflow instance") List<String> states,
      @RequestParam(value = "status", defaultValue = "") @ApiParam("Current status of workflow instance") List<WorkflowInstanceStatus> statuses,
      @RequestParam(value = "businessKey", required = false) @ApiParam("Business key for workflow instance") String businessKey,
      @RequestParam(value = "externalId", required = false) @ApiParam("External id for workflow instance") String externalId,
      @RequestParam(value = "include", required = false) @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @RequestParam(value = "maxResults", required = false) @ApiParam("Maximum number of workflow instances to be returned") Long maxResults,
      @RequestParam(value = "maxActions", required = false) @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    return super.listWorkflowInstances(ids, types, parentWorkflowId, parentActionId, states, statuses, businessKey, externalId,
        include, maxResults, maxActions, this.workflowInstances, this.listWorkflowConverter).iterator();
  }

  @PutMapping(path = "/{id}/signal", consumes = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Set workflow instance signal value", notes = "The service may be used for example to interrupt executing workflow instance.")
  @ApiResponses({ @ApiResponse(code = 200, message = "When operation was successful") })
  public ResponseEntity<?> setSignal(@ApiParam("Internal id for workflow instance") @PathVariable("id") long id,
      @RequestBody @Valid @ApiParam("New signal value") SetSignalRequest req) {
    boolean updated = workflowInstances.setSignal(id, ofNullable(req.signal), req.reason, WorkflowActionType.externalChange);
    return (updated ? ok("Signal was set successfully") : ok("Signal was not set"));
  }

  @PutMapping(path = "/{id}/wakeup", consumes = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Wake up sleeping workflow instance. If expected states are given, only wake up if the instance is in one of the expected states.")
  @ApiResponses({ @ApiResponse(code = 200, message = "When workflow wakeup was attempted") })
  public WakeupResponse wakeup(@ApiParam("Internal id for workflow instance") @PathVariable("id") long id,
      @RequestBody @Valid @ApiParam("Expected states") WakeupRequest req) {
    WakeupResponse response = new WakeupResponse();
    List<String> expectedStates = ofNullable(req.expectedStates).orElseGet(Collections::emptyList);
    response.wakeupSuccess = workflowInstances.wakeupWorkflowInstance(id, expectedStates);
    return response;
  }

}
