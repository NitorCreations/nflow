package com.nitorcreations.nflow.rest.v1.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        resp.actions.add(new Action(action.state, action.stateText, action.retryNo,
            action.executionStart, action.executionEnd));
      }
    }
    if(instance.stateVariables != null && !instance.stateVariables.isEmpty()) {
      resp.stateVariables = new LinkedHashMap<>();
      for(Entry<String, String> entry : instance.stateVariables.entrySet()) {
        resp.stateVariables.put(entry.getKey(), stringToJson(entry.getValue()));
      }
    }
    return resp;
  }

  private JsonNode stringToJson(String value) {
    try {
      return nflowObjectMapper.readTree(value);
    } catch (IOException e) {
      return new TextNode(value);
    }
  }
}
