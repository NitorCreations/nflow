package io.nflow.rest.v1.converter;

import static java.util.stream.Collectors.toMap;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import io.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse.Settings;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse.Signal;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse.TransitionDelays;
import io.nflow.rest.v1.msg.State;

@Component
public class ListWorkflowDefinitionConverter {

  public ListWorkflowDefinitionResponse convert(WorkflowDefinition definition) {
    ListWorkflowDefinitionResponse resp = new ListWorkflowDefinitionResponse();
    resp.type = definition.getType();
    resp.name = definition.getName();
    resp.description = definition.getDescription();
    resp.onError = definition.getErrorState().name();
    Map<String, State> states = definition.getStates().stream().collect(toMap(WorkflowState::name, this::toState));
    definition.getAllowedTransitions().forEach((key, targets) ->
        Optional.ofNullable(states.get(key)).ifPresent(s -> s.transitions.addAll(targets)));
    definition.getFailureTransitions().forEach((key, wfState) ->
        Optional.ofNullable(states.get(key)).ifPresent(s -> s.onFailure = wfState.name()));
    resp.states = states.values().toArray(new State[0]);

    WorkflowSettings workflowSettings = definition.getSettings();
    TransitionDelays transitionDelays = new TransitionDelays();
    transitionDelays.waitShort = workflowSettings.shortTransitionDelay;
    transitionDelays.minErrorWait = workflowSettings.minErrorTransitionDelay;
    transitionDelays.maxErrorWait = workflowSettings.maxErrorTransitionDelay;
    Settings settings = new Settings();
    settings.transitionDelaysInMilliseconds = transitionDelays;
    settings.maxRetries = workflowSettings.maxRetries;
    settings.historyDeletableAfter = workflowSettings.historyDeletableAfter.toPeriod();
    settings.defaultPriority = workflowSettings.defaultPriority;
    resp.settings = settings;

    resp.supportedSignals = definition.getSupportedSignals().entrySet().stream().map(entry -> {
      Signal signal = new Signal();
      signal.value = entry.getKey();
      signal.description = entry.getValue();
      return signal;
    }).toArray(size -> new Signal[size]);

    return resp;
  }

  private State toState(WorkflowState state) {
    return new State(state.name(), state.getType().name(), state.getDescription());
  }

  public ListWorkflowDefinitionResponse convert(StoredWorkflowDefinition storedDefinition) {
    ListWorkflowDefinitionResponse resp = new ListWorkflowDefinitionResponse();
    resp.type = storedDefinition.type;
    resp.description = storedDefinition.description;
    resp.onError = storedDefinition.onError;
    resp.states = storedDefinition.states.stream().map(state -> {
      State tmp = new State(state.id, state.type, state.description);
      tmp.transitions.addAll(state.transitions);
      tmp.onFailure = state.onFailure;
      return tmp;
    }).toArray(size -> new State[size]);
    resp.supportedSignals = storedDefinition.supportedSignals.stream().map(s -> {
      Signal signal = new Signal();
      signal.value = s.value;
      signal.description = s.description;
      return signal;
    }).toArray(size -> new Signal[size]);
    return resp;
  }
}
