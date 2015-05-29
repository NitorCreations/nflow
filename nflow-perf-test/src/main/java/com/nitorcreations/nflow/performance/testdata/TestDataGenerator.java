package com.nitorcreations.nflow.performance.testdata;

import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType.start;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.finished;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecutionFailed;
import static java.util.Arrays.asList;
import static java.util.Collections.reverse;
import static java.util.UUID.randomUUID;
import static org.joda.time.DateTime.now;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

/**
 * Generates imaginary workflow instances with action history based on the workflow definitions that are deployed to the
 * NflowPerfTestServer.
 */
public class TestDataGenerator {

  private final ExecutorDao executors;
  private final WorkflowDefinitionService workflowDefinitions;
  private final Random random;
  private int nextWorkflowId;
  private int nextActionId;

  public TestDataGenerator(ExecutorDao executors, WorkflowDefinitionService workflowDefinitions) {
    this.executors = executors;
    this.workflowDefinitions = workflowDefinitions;
    random = new Random();
  }

  public List<WorkflowInstance> generateWorkflowInstances(int count) {
    List<WorkflowInstance> result = new ArrayList<>();
    List<AbstractWorkflowDefinition<? extends WorkflowState>> definitions = workflowDefinitions.getWorkflowDefinitions();
    for (int i = 0; i < count; i++) {
      result.add(generateWorkflowInstance(definitions));
    }
    return result;
  }

  private WorkflowInstance generateWorkflowInstance(List<AbstractWorkflowDefinition<? extends WorkflowState>> definitions) {
    AbstractWorkflowDefinition<? extends WorkflowState> definition = selectRandom(definitions);
    DateTime nextActivation = now().minusYears(5).plusSeconds(random.nextInt(172800000)); // 2000 days
    WorkflowState state = selectRandomState(definition, nextActivation);
    WorkflowInstanceStatus status = selectRandomStatus(state, nextActivation);
    List<WorkflowInstanceAction> actions = generateWorkflowInstanceActions(definition, state,
        nextActivation);

    return new WorkflowInstance.Builder() //
        .setId(nextWorkflowId++) //
        .setType(definition.getType()) //
        .setBusinessKey(randomUUID().toString()) //
        .setExecutorGroup(executors.getExecutorGroup()) //
        .setExecutorId(executing == status ? executors.getExecutorId() : null) //
        .setExternalId(randomUUID().toString()) //
        .setCreated(nextActivation.minusMinutes(5 + random.nextInt(10))) //
        .setModified(nextActivation.minusMinutes(random.nextInt(5))) //
        .setNextActivation(asList(created, executing, inProgress).contains(status) ? nextActivation : null) //
        .setStarted(nextActivation.minusMinutes(random.nextInt(10))) //
        .setState(state.name()) //
        .setActions(actions) //
        .setStateText(randomUUID().toString()) //
        .setStatus(status) //
        .build();
  }

  private <T> T selectRandom(List<T> alternatives) {
    return alternatives.get((int) Math.floor(alternatives.size() * random.nextDouble()));
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private WorkflowState selectRandomState(AbstractWorkflowDefinition definition, DateTime instanceNextActivation) {
    if (instanceNextActivation.isAfterNow()) {
      return selectRandomState(definition.getStates(), asList(start));
    } else if (instanceNextActivation.isAfter(new DateTime().minusWeeks(1))) {
      return selectRandomState(definition.getStates(), asList(end, manual, normal));
    } else if (instanceNextActivation.isAfter(new DateTime().minusMonths(6))) {
      return selectRandomState(definition.getStates(), asList(end, manual));
    }
    return selectRandomState(definition.getStates(), asList(end));
  }

  private WorkflowState selectRandomState(Set<WorkflowState> allStates, List<WorkflowStateType> stateTypeFilter) {
    List<WorkflowState> filteredStates = new ArrayList<>();
    for (WorkflowState state : allStates) {
      if (stateTypeFilter.contains(state.getType())) {
        filteredStates.add(state);
      }
    }
    return selectRandom(filteredStates);
  }

  /**
   * Generates actions by backtracking from the randomized current state to a start state.
   */
  private List<WorkflowInstanceAction> generateWorkflowInstanceActions(
      AbstractWorkflowDefinition<? extends WorkflowState> definition,
      WorkflowState currentState, DateTime nextActivation) {
    List<WorkflowInstanceAction> result = new ArrayList<>();
    Set<WorkflowState> cyclePrevention = new HashSet<>();
    while (currentState.getType() != start) {
      WorkflowState previousState = null;
      boolean isFailureTransition = false;
      for (WorkflowState state : definition.getStates()) {
        List<String> allowedTransitions = definition.getAllowedTransitions().get(state.name());
        WorkflowState failureTransition = definition.getFailureTransitions().get(state.name());
        isFailureTransition = failureTransition == currentState;
        if (((allowedTransitions != null && allowedTransitions.contains(currentState.name())) || isFailureTransition)
            && !cyclePrevention.contains(state)) {
          previousState = state;
          cyclePrevention.add(state);
          break;
        }
      }
      // probably error state which is not reached through normal or failure transitions
      if (previousState == null) {
        previousState = definition.getInitialState();
      }
      int index = 0;
      int retryNo = isFailureTransition ? definition.getSettings().maxRetries : 0;
      while (retryNo > -1) {
        index++;
        result.add(new WorkflowInstanceAction.Builder() //
            .setWorkflowInstanceId(nextWorkflowId) //
            .setExecutionStart(nextActivation.minusSeconds(index * 10).minusSeconds(random.nextInt(10))) //
            .setExecutionEnd(nextActivation.minusSeconds(index * 10)) //
            .setExecutorId(executors.getExecutorId()) //
            .setRetryNo(retryNo--) //
            .setState(previousState.name()) //
            .setStateText(UUID.randomUUID().toString()) //
            .setType(isFailureTransition ? stateExecutionFailed : stateExecution) //
            .build());
      }
      currentState = previousState;
    }
    reverse(result);
    List<WorkflowInstanceAction> resultWithActionIds = new ArrayList<>();
    for (WorkflowInstanceAction action : result) {
      Map<String, String> updatedStateVariables = new HashMap<>();
      if (random.nextInt(2) > 0) {
        updatedStateVariables.put(Integer.toString(nextActionId), "\"" + randomUUID().toString() + "\"");
      }
      resultWithActionIds.add(new WorkflowInstanceAction.Builder(action).setId(nextActionId++)
          .setUpdatedStateVariables(updatedStateVariables).build());
    }
    return resultWithActionIds;
  }

  private WorkflowInstanceStatus selectRandomStatus(WorkflowState state, DateTime nextActivation) {
    if (state.getType() == end) {
      return finished;
    } else if (state.getType() == normal) {
      return inProgress;
    } else if (state.getType() == manual) {
      return WorkflowInstanceStatus.manual;
    } else if (now().isBefore(nextActivation)) {
      return created;
    } else if (new Interval(now().minusMinutes(10), now().plusMinutes(10)).contains(nextActivation)) {
      return executing;
    }
    return finished;
  }

  public void setCurrentWorkflowId(int currentWorkflowId) {
    nextWorkflowId = currentWorkflowId + 1;
  }

  public void setCurrentActionId(int currentActionId) {
    nextActionId = currentActionId + 1;
  }

}
