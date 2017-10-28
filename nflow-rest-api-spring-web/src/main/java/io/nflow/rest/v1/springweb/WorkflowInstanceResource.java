package io.nflow.rest.v1.springweb;

import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_INSTANCE_PATH;
import static java.util.Optional.ofNullable;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.net.URI;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@RestController
@RequestMapping(value = NFLOW_WORKFLOW_INSTANCE_PATH, produces = APPLICATION_JSON_VALUE)
@Api("nFlow workflow instance management")
@Component
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

  @PutMapping(consumes = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Submit new workflow instance")
  @ApiResponses(@ApiResponse(code = 201, message = "Workflow was created", response = CreateWorkflowInstanceResponse.class))
  public ResponseEntity<CreateWorkflowInstanceResponse> createWorkflowInstance(
      @RequestBody @ApiParam(value = "Submitted workflow instance information", required = true) CreateWorkflowInstanceRequest req) {
    WorkflowInstance instance = createWorkflowConverter.convert(req);
    int id = workflowInstances.insertWorkflowInstance(instance);
    instance = workflowInstances.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null);
    return ResponseEntity.created(URI.create(String.valueOf(id))).body(createWorkflowConverter.convert(instance));
  }

  @PutMapping(path = "/{id}", consumes = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Update workflow instance", notes = "The service is typically used in manual state "
      + "transition via nFlow Explorer or a business UI.")
  @ApiResponses({ @ApiResponse(code = 204, message = "If update was successful"),
      @ApiResponse(code = 409, message = "If workflow was executing and no update was done") })
  public ResponseEntity<?> updateWorkflowInstance(@ApiParam("Internal id for workflow instance") @PathVariable("id") int id,
      @RequestBody @ApiParam("Submitted workflow instance information") UpdateWorkflowInstanceRequest req) {
    boolean updated = super.updateWorkflowInstance(id, req, workflowInstanceFactory, workflowInstances);
    return (updated ? ResponseEntity.noContent() : ResponseEntity.status(HttpStatus.CONFLICT)).build();
  }

  @GetMapping(path = "/{id}")
  @ApiOperation(value = "Fetch a workflow instance", notes = "Fetch full state and action history of a single workflow instance.")
  public ResponseEntity<ListWorkflowInstanceResponse> fetchWorkflowInstance(
      @ApiParam("Internal id for workflow instance") @PathVariable("id") int id,
      @RequestParam("include") @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @RequestParam("maxActions") @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    try {
      return ResponseEntity.ok().body(super.fetchWorkflowInstance(id, include, maxActions,
          this.workflowInstances, this.listWorkflowConverter));
    } catch (@SuppressWarnings("unused") EmptyResultDataAccessException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping
  @ApiOperation(value = "List workflow instances", response = ListWorkflowInstanceResponse.class, responseContainer = "List")
  public Collection<ListWorkflowInstanceResponse> listWorkflowInstances(
      @RequestParam("id") @ApiParam("Internal id of workflow instance") List<Integer> ids,
      @RequestParam("type") @ApiParam("Workflow definition type of workflow instance") List<String> types,
      @RequestParam("parentWorkflowId") @ApiParam("Id of parent workflow instance") Integer parentWorkflowId,
      @RequestParam("parentActionId") @ApiParam("Id of parent workflow instance action") Integer parentActionId,
      @RequestParam("state") @ApiParam("Current state of workflow instance") List<String> states,
      @RequestParam("status") @ApiParam("Current status of workflow instance") List<WorkflowInstanceStatus> statuses,
      @RequestParam("businessKey") @ApiParam("Business key for workflow instance") String businessKey,
      @RequestParam("externalId") @ApiParam("External id for workflow instance") String externalId,
      @RequestParam("include") @ApiParam(value = INCLUDE_PARAM_DESC, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true) String include,
      @RequestParam("maxResults") @ApiParam("Maximum number of workflow instances to be returned") Long maxResults,
      @RequestParam("maxActions") @ApiParam("Maximum number of actions returned for each workflow instance") Long maxActions) {
    return super.listWorkflowInstances(ids, types, parentWorkflowId, parentActionId, states,
        statuses, businessKey, externalId, include, maxResults, maxActions,
        this.workflowInstances, this.listWorkflowConverter);
  }

  @PutMapping(path = "/{id}/signal", consumes = APPLICATION_JSON_VALUE)
  @ApiOperation(value = "Set workflow instance signal value", notes = "The service may be used for example to interrupt executing workflow instance.")
  @ApiResponses({ @ApiResponse(code = 200, message = "When operation was successful") })
  public ResponseEntity<?> setSignal(@ApiParam("Internal id for workflow instance") @PathVariable("id") int id,
      @RequestBody @Valid @ApiParam("New signal value") SetSignalRequest req) {
    boolean updated = workflowInstances.setSignal(id, ofNullable(req.signal), req.reason, WorkflowActionType.externalChange);
    return (updated ? ResponseEntity.ok("Signal was set successfully") : ResponseEntity.ok("Signal was not set"));
  }

}
