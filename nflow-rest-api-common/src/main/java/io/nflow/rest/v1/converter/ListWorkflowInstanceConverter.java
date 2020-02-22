package io.nflow.rest.v1.converter;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;

@Component
public class ListWorkflowInstanceConverter {
  private static final Logger logger = LoggerFactory.getLogger(ListWorkflowInstanceConverter.class);

  @Inject
  private ObjectMapper nflowRestObjectMapper;

  public ListWorkflowInstanceResponse convert(WorkflowInstance instance, Set<WorkflowInstanceInclude> includes) {
    ListWorkflowInstanceResponse resp = new ListWorkflowInstanceResponse();
    resp.id = instance.id;
    resp.status = instance.status.name();
    resp.type = instance.type;
    resp.priority = instance.priority;
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
    resp.signal = instance.signal.orElse(null);
    resp.archived = instance.isArchived ? TRUE : null;
    if (includes.contains(WorkflowInstanceInclude.ACTIONS)) {
      resp.actions = new ArrayList<>();
      for (WorkflowInstanceAction action : instance.actions) {
        if (includes.contains(WorkflowInstanceInclude.ACTION_STATE_VARIABLES)) {
          resp.actions.add(new Action(action.id, action.type.name(), action.state, action.stateText, action.retryNo,
              action.executionStart, action.executionEnd, action.executorId, stateVariablesToJson(action.updatedStateVariables)));
        } else {
          resp.actions.add(new Action(action.id, action.type.name(), action.state, action.stateText, action.retryNo,
              action.executionStart, action.executionEnd, action.executorId));
        }
      }
    }
    if (includes.contains(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES)) {
      resp.stateVariables = stateVariablesToJson(instance.stateVariables);
    }
    if (includes.contains(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS)) {
      resp.childWorkflows = instance.childWorkflows;
    }
    return resp;
  }

  private Map<String, Object> stateVariablesToJson(Map<String, String> stateVariables) {
    if (isEmpty(stateVariables)) {
      return null;
    }
    return stateVariables.entrySet().stream().collect(toMap(Entry::getKey, this::stringToJson));
  }

  private JsonNode stringToJson(Entry<String, String> entry) {
    try {
      return nflowRestObjectMapper.readTree(entry.getValue());
    } catch (JsonProcessingException e) {
      logger.debug("Failed to parse state variable {} value as JSON, returning value as unparsed string: {}: {}", entry.getKey(),
          e.getClass().getSimpleName(), e.getMessage());
      return new TextNode(entry.getValue());
    }
  }
}
