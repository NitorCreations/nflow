package com.nitorcreations.nflow.rest.v0.converter;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowState;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowDefinitionResponse;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowDefinitionResponse.Transition;

@Component
public class ListWorkflowDefinitionConverter {

  public ListWorkflowDefinitionResponse convert(WorkflowDefinition<? extends WorkflowState> definition) {
    ListWorkflowDefinitionResponse resp = new ListWorkflowDefinitionResponse();
    resp.type = definition.getType();
    resp.states.initial = asList(definition.getInitialState().name());
    resp.states.onError = definition.getErrorState().name();
    Map<String, Transition> transitions = new HashMap<>();
    for (Entry<String,String> entry : definition.getAllowedTransitions().entrySet()) {
      Transition t = getTransition(entry.getKey(), transitions);
      t.targets.add(entry.getValue());
    }
    for (Entry<String,String> entry : definition.getFailureTransitions().entrySet()) {
      Transition t = getTransition(entry.getKey(), transitions);
      t.onFailure = entry.getValue();
    }
    resp.states.transitions = new ArrayList<>(transitions.values());
    return resp;
  }

  private Transition getTransition(String source, Map<String,Transition> transitions) {
    Transition t = transitions.get(source);
    if (t == null) {
      t = new Transition();
      t.source = source;
      transitions.put(source, t);
    }
    return t;
  }

}
