package io.nflow.rest.v1.springweb;

import static io.nflow.rest.config.springweb.PathConstants.NFLOW_SPRING_WEB_PATH_PREFIX;
import static io.nflow.rest.v1.ResourcePaths.NFLOW_WORKFLOW_INSTANCE_PATH;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import java.net.URI;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.config.springweb.SchedulerService;
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
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = NFLOW_SPRING_WEB_PATH_PREFIX + NFLOW_WORKFLOW_INSTANCE_PATH, produces = APPLICATION_JSON_VALUE)
@OpenAPIDefinition(info = @Info(
        title = "nFlow workflow instance management"
))
@Component
public class WorkflowInstanceResource extends SpringWebResource {

  private final WorkflowInstanceService workflowInstances;
  private final CreateWorkflowConverter createWorkflowConverter;
  private final ListWorkflowInstanceConverter listWorkflowConverter;
  private final WorkflowInstanceFactory workflowInstanceFactory;
  private final WorkflowInstanceDao workflowInstanceDao;

  @Inject
  public WorkflowInstanceResource(SchedulerService scheduler, WorkflowInstanceService workflowInstances,
      CreateWorkflowConverter createWorkflowConverter, ListWorkflowInstanceConverter listWorkflowConverter,
      WorkflowInstanceFactory workflowInstanceFactory, WorkflowInstanceDao workflowInstanceDao) {
    super(scheduler);
    this.workflowInstances = workflowInstances;
    this.createWorkflowConverter = createWorkflowConverter;
    this.listWorkflowConverter = listWorkflowConverter;
    this.workflowInstanceFactory = workflowInstanceFactory;
    this.workflowInstanceDao = workflowInstanceDao;
  }

  @PutMapping(consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Submit new workflow instance")
  @ApiResponses({ @ApiResponse(responseCode = "201", description = "Workflow was created", content = @Content(schema = @Schema(implementation = CreateWorkflowInstanceResponse.class))),
          @ApiResponse(responseCode = "400", description = "If instance could not be created, for example when state variable value was too long") })
  public Mono<ResponseEntity<CreateWorkflowInstanceResponse>> createWorkflowInstance(
          @RequestBody @Parameter(description = "Submitted workflow instance information", required = true) CreateWorkflowInstanceRequest req) {
    return handleExceptions(() -> wrapBlocking(() -> {
      WorkflowInstance instance = createWorkflowConverter.convert(req);
      long id = workflowInstances.insertWorkflowInstance(instance);
      instance = workflowInstances.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null);
      return created(URI.create(String.valueOf(id))).body(createWorkflowConverter.convert(instance));
    }));
  }

  @PutMapping(path = "/id/{id}", consumes = APPLICATION_JSON_VALUE)
  @Operation(summary = "Update workflow instance", description = "The service is typically used in manual state "
      + "transition via nFlow Explorer or a business UI.")
  @ApiResponses({ @ApiResponse(responseCode = "204", description = "If update was successful"),
          @ApiResponse(responseCode = "400", description = "If instance could not be updated, for example when state variable value was too long"),
          @ApiResponse(responseCode = "409", description = "If workflow was executing and no update was done") })
  public Mono<ResponseEntity<?>> updateWorkflowInstance(@Parameter(description = "Internal id for workflow instance") @PathVariable("id") long id,
                                                  @RequestBody @Parameter(description = "Submitted workflow instance information") UpdateWorkflowInstanceRequest req) {
    return handleExceptions(() -> wrapBlocking(() -> {
      boolean updated = super.updateWorkflowInstance(id, req, workflowInstanceFactory, workflowInstances, workflowInstanceDao);
      return (updated ? noContent() : status(CONFLICT)).build();
    }));
  }

  @GetMapping(path = "/id/{id}")
  @ApiOperation(value = "Fetch a workflow instance", notes = "Fetch full state and action history of a single workflow instance.")
  @ApiResponses({ @ApiResponse(code = 200, response = ListWorkflowInstanceResponse.class, message = "If instance was found"),
      @ApiResponse(code = 404, message = "If instance was not found") })
  @SuppressFBWarnings(value = "LEST_LOST_EXCEPTION_STACK_TRACE", justification = "The empty result exception contains no useful information")
  public Mono<ResponseEntity<ListWorkflowInstanceResponse>> fetchWorkflowInstance(
          @Parameter(description = "Internal id for workflow instance") @PathVariable("id") long id,
          @RequestParam(value = "include", required = false) @Parameter(description = INCLUDE_PARAM_DESC/*, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true*/) String include,
          @RequestParam(value = "queryArchive", required = false, defaultValue = QUERY_ARCHIVED_DEFAULT_STR) @Parameter("Query also the archive if not found from main tables") boolean queryArchive,
          @RequestParam(value = "maxActions", required = false) @Parameter(description = "Maximum number of actions returned for each workflow instance") Long maxActions) {
    return handleExceptions(() -> wrapBlocking(
        () -> ok(super.fetchWorkflowInstance(id, include, maxActions, queryArchive, this.workflowInstances, this.listWorkflowConverter))));
  }

  @GetMapping
  @Operation(summary = "List workflow instances")
  @ApiResponse(content = @Content(array = @ArraySchema(schema = @Schema(implementation = ListWorkflowInstanceResponse.class))))
  public Mono<ResponseEntity<Stream<ListWorkflowInstanceResponse>>> listWorkflowInstances(
          @RequestParam(value = "id", defaultValue = "") @Parameter(description ="Internal id of workflow instance") List<Long> ids,
          @RequestParam(value = "type", defaultValue = "") @Parameter(description ="Workflow definition type of workflow instance") List<String> types,
          @RequestParam(value = "parentWorkflowId", required = false) @Parameter(description ="Id of parent workflow instance") Long parentWorkflowId,
          @RequestParam(value = "parentActionId", required = false) @Parameter(description ="Id of parent workflow instance action") Long parentActionId,
          @RequestParam(value = "state", defaultValue = "") @Parameter(description ="Current state of workflow instance") List<String> states,
          @RequestParam(value = "status", defaultValue = "") @Parameter(description ="Current status of workflow instance") List<WorkflowInstanceStatus> statuses,
          @RequestParam(value = "businessKey", required = false) @Parameter(description ="Business key for workflow instance") String businessKey,
          @RequestParam(value = "externalId", required = false) @Parameter(description ="External id for workflow instance") String externalId,
          @RequestParam(value = "include", required = false) @Parameter(description = INCLUDE_PARAM_DESC/*, allowableValues = INCLUDE_PARAM_VALUES, allowMultiple = true*/) String include,
          @RequestParam(value = "maxResults", required = false) @Parameter(description ="Maximum number of workflow instances to be returned") Long maxResults,
          @RequestParam(value = "maxActions", required = false) @Parameter(description ="Maximum number of actions returned for each workflow instance") Long maxActions,
      @RequestParam(value = "stateVariableKey", required = false) @Parameter("Key of state variable that must exist for workflow instance") String stateVariableKey,
      @RequestParam(value = "stateVariableValue", required = false) @Parameter("Current value of state variable defined by stateVariableKey") String stateVariableValue,
      @RequestParam(value = "queryArchive", required = false, defaultValue = QUERY_ARCHIVED_DEFAULT_STR) @Parameter("Query also the archive if not enough results found from main tables") boolean queryArchive) {
    return handleExceptions(() -> wrapBlocking(
        () -> ok(super.listWorkflowInstances(ids, types, parentWorkflowId, parentActionId, states, statuses, businessKey,
            externalId, stateVariableKey, stateVariableValue, include, maxResults, maxActions, queryArchive, this.workflowInstances,
            this.listWorkflowConverter).iterator())));
  }

  @PutMapping(path = "/{id}/signal", consumes = APPLICATION_JSON_VALUE)
    @Operation(summary = "Set workflow instance signal value", description = "The service may be used for example to interrupt executing workflow instance.")
    @ApiResponse(responseCode = "200", description = "When operation was successful")
  public Mono<ResponseEntity<SetSignalResponse> setSignal(@Parameter(description ="Internal id for workflow instance") @PathVariable("id") long id,
    @RequestBody @Valid @Parameter(description ="New signal value") SetSignalRequest req) {
    return handleExceptions(() -> wrapBlocking(() -> {
      SetSignalResponse response = new SetSignalResponse();
      response.setSignalSuccess = workflowInstances.setSignal(id, ofNullable(req.signal), req.reason,
          WorkflowActionType.externalChange);
      return ok(response);
    }));
  }

  @PutMapping(path = "/{id}/wakeup", consumes = APPLICATION_JSON_VALUE)
  @Operation(description = "Wake up sleeping workflow instance. If expected states are given, only wake up if the instance is in one of the expected states.")
  @ApiResponse(responseCode = "200", description = "When workflow wakeup was attempted")
  public Mono<ResponseEntity<WakeupResponse>> wakeup(@Parameter(description = "Internal id for workflow instance") @PathVariable("id") long id,
                               @RequestBody @Valid @Parameter(description = "Expected states") WakeupRequest req) {
    return handleExceptions(() -> wrapBlocking(() -> {
      WakeupResponse response = new WakeupResponse();
      List<String> expectedStates = ofNullable(req.expectedStates).orElseGet(Collections::emptyList);
      response.wakeupSuccess = workflowInstances.wakeupWorkflowInstance(id, expectedStates);
      return ok(response);
    }));
}
