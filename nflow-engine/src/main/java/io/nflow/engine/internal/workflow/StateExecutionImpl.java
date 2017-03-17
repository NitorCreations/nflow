package io.nflow.engine.internal.workflow;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.Assert.notNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.util.Assert;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.model.ModelObject;
import io.nflow.engine.service.WorkflowInstanceService;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

public class StateExecutionImpl extends ModelObject implements StateExecution {

  private static final Logger LOG = getLogger(StateExecutionImpl.class);
  private final WorkflowInstance instance;
  private final ObjectStringMapper objectMapper;
  private final WorkflowInstanceDao workflowDao;
  private final WorkflowInstancePreProcessor workflowInstancePreProcessor;
  private final WorkflowInstanceService workflowInstanceService;
  private DateTime nextActivation;
  private String nextState;
  private String nextStateReason;
  private boolean isRetry;
  private Throwable thrown;
  private boolean isFailed;
  private boolean isRetryCountExceeded;
  private boolean isStateProcessInvoked = false;
  private final List<WorkflowInstance> newChildWorkflows = new LinkedList<>();
  private final List<WorkflowInstance> newWorkflows = new LinkedList<>();
  private boolean createAction = true;
  private String[] wakeUpParentStates;

  public StateExecutionImpl(WorkflowInstance instance, ObjectStringMapper objectMapper, WorkflowInstanceDao workflowDao,
      WorkflowInstancePreProcessor workflowInstancePreProcessor, WorkflowInstanceService workflowInstanceService) {
    this.instance = instance;
    this.objectMapper = objectMapper;
    this.workflowDao = workflowDao;
    this.workflowInstancePreProcessor = workflowInstancePreProcessor;
    this.workflowInstanceService = workflowInstanceService;
  }

  public DateTime getNextActivation() {
    return this.nextActivation;
  }

  public String getNextState() {
    return this.nextState;
  }

  public String getNextStateReason() {
    return this.nextStateReason;
  }

  public String getCurrentStateName() {
    return instance.state;
  }

  @Override
  public int getWorkflowInstanceId() {
    return instance.id;
  }

  @Override
  public String getWorkflowInstanceExternalId() {
    return instance.externalId;
  }

  @Override
  public String getBusinessKey() {
    return instance.businessKey;
  }

  @Override
  public int getRetries() {
    return instance.retries;
  }

  @Override
  public String getVariable(String name) {
    return getVariable(name, (String) null);
  }

  @Override
  public <T> T getVariable(String name, Class<T> type) {
    return getVariable(name, type, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getVariable(String name, Class<T> type, T defaultValue) {
    if (instance.stateVariables.containsKey(name)) {
      return (T) objectMapper.convertToObject(type, name, instance.stateVariables.get(name));
    }
    return defaultValue;
  }

  @Override
  public String getVariable(String name, String defaultValue) {
    if (instance.stateVariables.containsKey(name)) {
      return instance.stateVariables.get(name);
    }
    return defaultValue;
  }

  @Override
  public void setVariable(String name, String value) {
    instance.stateVariables.put(name, value);
  }

  @Override
  public void setVariable(String name, Object value) {
    setVariable(name, objectMapper.convertFromObject(name, value));
  }

  public void setNextActivation(DateTime activation) {
    this.nextActivation = activation;
  }

  public void setNextState(WorkflowState state) {
    notNull(state, "Next state can not be null");
    this.nextState = state.name();
  }

  public void setNextStateReason(String reason) {
    this.nextStateReason = reason;
  }

  public boolean isRetry() {
    return isRetry;
  }

  public void setRetry(boolean isRetry) {
    this.isRetry = isRetry;
  }

  public boolean isFailed() {
    return isFailed;
  }

  public Throwable getThrown() {
    return thrown;
  }

  public void setFailed() {
    isFailed = true;
  }

  public void setFailed(Throwable t) {
    isFailed = true;
    thrown = t;
  }

  public boolean isRetryCountExceeded() {
    return isRetryCountExceeded;
  }

  public void setRetryCountExceeded() {
    isRetryCountExceeded = true;
  }

  @Override
  public void addChildWorkflows(WorkflowInstance... childWorkflows) {
    Assert.notNull(childWorkflows, "childWorkflows can not be null");
    for (WorkflowInstance child : childWorkflows) {
      Assert.notNull(child, "childWorkflow can not be null");
      WorkflowInstance processedChild = workflowInstancePreProcessor.process(child);
      newChildWorkflows.add(processedChild);
    }
  }

  @Override
  public void addWorkflows(WorkflowInstance... workflows) {
    Assert.notNull(workflows, "workflows can not be null");
    for (WorkflowInstance workflow : workflows) {
      Assert.notNull(workflow, "workflow can not be null");
      WorkflowInstance processedInstance = workflowInstancePreProcessor.process(workflow);
      newWorkflows.add(processedInstance);
    }
  }

  public List<WorkflowInstance> getNewChildWorkflows() {
    return unmodifiableList(newChildWorkflows);
  }

  public List<WorkflowInstance> getNewWorkflows() {
    return unmodifiableList(newWorkflows);
  }

  @Override
  public List<WorkflowInstance> queryChildWorkflows(QueryWorkflowInstances query) {
    QueryWorkflowInstances restrictedQuery = new QueryWorkflowInstances.Builder(query).setParentWorkflowId(instance.id).build();
    return workflowDao.queryWorkflowInstances(restrictedQuery);
  }

  @Override
  public List<WorkflowInstance> getAllChildWorkflows() {
    return queryChildWorkflows(new QueryWorkflowInstances.Builder().build());
  }

  @Override
  public void wakeUpParentWorkflow(String... expectedStates) {
    if (instance.parentWorkflowId == null) {
      LOG.warn("wakeUpParentWorkflow called on non-child workflow");
      return;
    }
    wakeUpParentStates = expectedStates;
  }

  public Optional<List<String>> getWakeUpParentWorkflowStates() {
    return Optional.ofNullable(wakeUpParentStates).map(s -> asList(s));
  }

  @Override
  public WorkflowInstance.Builder workflowInstanceBuilder() {
    return new WorkflowInstance.Builder(this.objectMapper).setNextActivation(now());
  }

  public void setStateProcessInvoked(boolean isStateProcessInvoked) {
    this.isStateProcessInvoked = isStateProcessInvoked;
  }

  public boolean isStateProcessInvoked() {
    return isStateProcessInvoked;
  }

  @Override
  public void setCreateAction(boolean createAction) {
    this.createAction = createAction;
  }

  public boolean createAction() {
    return createAction;
  }

  @Override
  public Optional<Integer> getSignal() {
    return workflowDao.getSignal(instance.id);
  }

  @Override
  public void setSignal(Optional<Integer> signal, String reason) {
    Assert.notNull(signal, "signal can not be null, use Optional.empty() to clear the signal value");
    workflowInstanceService.setSignal(instance.id, signal, reason, WorkflowActionType.stateExecution);
  }

  @Override
  public Optional<Integer> getParentId() {
    return Optional.ofNullable(instance.parentWorkflowId);
  }

}
