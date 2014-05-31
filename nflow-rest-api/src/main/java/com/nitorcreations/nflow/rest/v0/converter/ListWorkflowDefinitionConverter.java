package com.nitorcreations.nflow.rest.v0.converter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowDefinitionResponse;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowDefinitionResponse.Settings;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowDefinitionResponse.TransitionDelays;
import com.nitorcreations.nflow.rest.v0.msg.State;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class ListWorkflowDefinitionConverter {

  @SuppressFBWarnings(value = "BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", justification = "cast is safe")
  public ListWorkflowDefinitionResponse convert(WorkflowDefinition<? extends WorkflowState> definition) {
    ListWorkflowDefinitionResponse resp = new ListWorkflowDefinitionResponse();
    resp.type = definition.getType();
    resp.name = definition.getName();
    resp.description = definition.getDescription();
    resp.onError = definition.getErrorState().name();
    Map<String, State> states = new LinkedHashMap<>();
    for (WorkflowState state : definition.getStates()) {
      states.put(state.name(), new State(state.name(), state.getType().name(),
          state.getName(), state.getDescription()));
    }
    for (Entry<String, List<String>> entry : definition.getAllowedTransitions().entrySet()) {
      State state = states.get(entry.getKey());
      for(String targetState : entry.getValue()) {
        state.transitions.add(targetState);
      }
    }
    for (Entry<String,String> entry : definition.getFailureTransitions().entrySet()) {
      State state = states.get(entry.getKey());
      state.onFailure = entry.getValue();
    }
    resp.states = states.values().toArray(new State[states.values().size()]);

    WorkflowSettings workflowSettings = definition.getSettings();
    TransitionDelays transitionDelays = new TransitionDelays();
    transitionDelays.immediate = workflowSettings.getImmediateTransitionDelay();
    transitionDelays.waitShort = workflowSettings.getShortTransitionDelay();
    transitionDelays.waitError = workflowSettings.getErrorTransitionDelay();
    Settings settings = new Settings();
    settings.transitionDelaysInMilliseconds = transitionDelays;
    settings.maxRetries = workflowSettings.getMaxRetries();
    resp.settings = settings;

    return resp;
  }
}
