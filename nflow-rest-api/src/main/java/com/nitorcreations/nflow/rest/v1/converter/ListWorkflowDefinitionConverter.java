package com.nitorcreations.nflow.rest.v1.converter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.workflow.StoredWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse.Settings;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse.TransitionDelays;
import com.nitorcreations.nflow.rest.v1.msg.State;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class ListWorkflowDefinitionConverter {

  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", justification = "cast is safe")
  public ListWorkflowDefinitionResponse convert(AbstractWorkflowDefinition<? extends WorkflowState> definition) {
    ListWorkflowDefinitionResponse resp = new ListWorkflowDefinitionResponse();
    resp.type = definition.getType();
    resp.name = definition.getName();
    resp.description = definition.getDescription();
    resp.onError = definition.getErrorState().name();
    Map<String, State> states = new LinkedHashMap<>();
    for (WorkflowState state : definition.getStates()) {
      states.put(state.name(), new State(state.name(), state.getType().name(),
          state.getDescription()));
    }
    for (Entry<String, List<String>> entry : definition.getAllowedTransitions().entrySet()) {
      State state = states.get(entry.getKey());
      for(String targetState : entry.getValue()) {
        state.transitions.add(targetState);
      }
    }
    for (Entry<String, WorkflowState> entry : definition.getFailureTransitions().entrySet()) {
      State state = states.get(entry.getKey());
      state.onFailure = entry.getValue().name();
    }
    resp.states = states.values().toArray(new State[states.values().size()]);

    WorkflowSettings workflowSettings = definition.getSettings();
    TransitionDelays transitionDelays = new TransitionDelays();
    transitionDelays.immediate = workflowSettings.immediateTransitionDelay;
    transitionDelays.waitShort = workflowSettings.shortTransitionDelay;
    transitionDelays.minErrorWait = workflowSettings.minErrorTransitionDelay;
    transitionDelays.maxErrorWait = workflowSettings.maxErrorTransitionDelay;
    Settings settings = new Settings();
    settings.transitionDelaysInMilliseconds = transitionDelays;
    settings.maxRetries = workflowSettings.maxRetries;
    resp.settings = settings;

    return resp;
  }

  public ListWorkflowDefinitionResponse convert(StoredWorkflowDefinition storedDefinition) {
    ListWorkflowDefinitionResponse resp = new ListWorkflowDefinitionResponse();
    resp.type = storedDefinition.type;
    resp.description = storedDefinition.description;
    resp.onError = storedDefinition.onError;
    List<State> states = new ArrayList<>();
    for (StoredWorkflowDefinition.State state : storedDefinition.states) {
      State tmp = new State(state.id, state.type, state.description);
      tmp.transitions.addAll(state.transitions);
      tmp.onFailure = state.onFailure;
      states.add(tmp);
    }
    resp.states = states.toArray(new State[states.size()]);
    return resp;
  }
}
