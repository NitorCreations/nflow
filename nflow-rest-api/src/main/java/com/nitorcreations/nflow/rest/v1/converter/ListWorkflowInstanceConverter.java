package com.nitorcreations.nflow.rest.v1.converter;

import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final Logger logger = LoggerFactory.getLogger(ListWorkflowInstanceConverter.class);

  @Inject
  private ObjectMapper nflowRestObjectMapper;

  public ListWorkflowInstanceResponse convert(WorkflowInstance instance, QueryWorkflowInstances query) {
    ListWorkflowInstanceResponse resp = new ListWorkflowInstanceResponse();
    resp.id = instance.id;
    resp.status = instance.status.name();
    resp.type = instance.type;
    resp.parentWorkflowId = instance.parentWorkflowId;
    resp.parentActionId = instance.parentActionId;
    resp.businessKey = instance.businessKey;
    resp.externalId = instance.externalId;
    resp.state = instance.state;
    resp.stateText = instance.stateText;
    resp.nextActivation = instance.nextActivation;
    resp.created = instance.created;
    resp.modified = instance.modified;
    resp.started = instance.started;
    resp.retries = instance.retries;
    if (query.includeActions) {
      resp.actions = new ArrayList<>();
      for (WorkflowInstanceAction action : instance.actions) {
        if(query.includeActionStateVariables) {
          resp.actions.add(new Action(action.id, action.type.name(), action.state, action.stateText, action.retryNo,
              action.executionStart, action.executionEnd, action.executorId, stateVariablesToJson(action.updatedStateVariables)));
        } else {
          resp.actions.add(new Action(action.id, action.type.name(), action.state, action.stateText, action.retryNo,
              action.executionStart, action.executionEnd, action.executorId));
        }
      }
    }
    if (query.includeCurrentStateVariables) {
      resp.stateVariables = stateVariablesToJson(instance.stateVariables);
    }
    if (query.includeChildWorkflows) {
      resp.childWorkflows = instance.childWorkflows;
    }
    return resp;
  }

  private Map<String, Object> stateVariablesToJson(Map<String, String>  stateVariables) {
    if(isEmpty(stateVariables)) {
      return null;
    }
    Map<String, Object> jsonStateVariables = new LinkedHashMap<>();
    for(Entry<String, String> entry : stateVariables.entrySet()) {
      jsonStateVariables.put(entry.getKey(), stringToJson(entry.getKey(), entry.getValue()));
    }

    return jsonStateVariables;
  }

  private JsonNode stringToJson(String key, String value) {
    try {
      return nflowRestObjectMapper.readTree(value);
    } catch (IOException e) {
      logger.warn("Failed to parse state variable {} value as JSON, returning value as unparsed string: {}: {}",
    		  key, e.getClass().getSimpleName(), e.getMessage());
      return new TextNode(value);
    }
  }
}
