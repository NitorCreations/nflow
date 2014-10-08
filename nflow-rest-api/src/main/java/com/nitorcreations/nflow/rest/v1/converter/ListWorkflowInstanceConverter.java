package com.nitorcreations.nflow.rest.v1.converter;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.rest.v1.msg.Action;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;

@Component
public class ListWorkflowInstanceConverter {

  @Inject
  @Named("nflowObjectMapper")
  private ObjectMapper nflowObjectMapper;

  public ListWorkflowInstanceResponse convert(WorkflowInstance instance, QueryWorkflowInstances query) {
    ListWorkflowInstanceResponse resp = new ListWorkflowInstanceResponse();
    resp.id = instance.id;
    resp.type = instance.type;
    resp.businessKey = instance.businessKey;
    resp.externalId = instance.externalId;
    resp.state = instance.state;
    resp.stateText = instance.stateText;
    resp.nextActivation = instance.nextActivation;
    if (query.includeActions) {
      resp.actions = new ArrayList<>();
      for (WorkflowInstanceAction action : instance.actions) {
        if(query.includeActionStateVariables) {
          resp.actions.add(new Action(action.state, action.stateText, action.retryNo,
              action.executionStart, action.executionEnd, stateVariablesToJson(action.updatedStateVariables)));
        } else {
          resp.actions.add(new Action(action.state, action.stateText, action.retryNo,
              action.executionStart, action.executionEnd));
        }
      }
    }
    if(query.includeCurrentStateVariables) {
      resp.stateVariables = stateVariablesToJson(instance.stateVariables);
    }
    return resp;
  }

  private Map<String, Object> stateVariablesToJson(Map<String, String>  stateVariables) {
    if(isEmpty(stateVariables)) {
      return null;
    }
    Map<String, Object> jsonStateVariables = new LinkedHashMap<>();
    for(Entry<String, String> entry : stateVariables.entrySet()) {
      jsonStateVariables.put(entry.getKey(), stringToJson(entry.getValue()));
    }

    return jsonStateVariables;
  }

  private JsonNode stringToJson(String value) {
    try {
      return nflowObjectMapper.readTree(value);
    } catch (IOException e) {
      return new TextNode(value);
    }
  }
}
