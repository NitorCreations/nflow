package com.nitorcreations.nflow.rest.v0.converter;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.service.WorkflowInstance;
import com.nitorcreations.nflow.engine.service.WorkflowInstanceAction;
import com.nitorcreations.nflow.rest.v0.msg.Action;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse;

@Component
public class ListWorkflowInstanceConverter {

  public ListWorkflowInstanceResponse convert(WorkflowInstance instance, QueryWorkflowInstances query) {
    ListWorkflowInstanceResponse resp = new ListWorkflowInstanceResponse();
    resp.id = instance.id;
    resp.type = instance.type;
    resp.businessKey = instance.businessKey;
    resp.state = instance.state;
    resp.stateText = instance.stateText;
    resp.nextActivation = instance.nextActivation;
    if (query.includeActions) {
      resp.actions = new ArrayList<>();
      for (WorkflowInstanceAction action : instance.actions) {
        resp.actions.add(new Action(action.state, action.stateText, action.retryNo,
            action.executionStart.getMillis(), action.executionEnd.getMillis()));
      }
    }
    return resp;
  }

}
