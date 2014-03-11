package com.nitorcreations.nflow.rest.v0.converter;

import java.util.ArrayList;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.domain.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse.Action;

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
      addActions(resp, instance);
    }
    return resp;
  }

  private void addActions(ListWorkflowInstanceResponse resp, WorkflowInstance instance) {
    resp.actions = new ArrayList<>();
    for (WorkflowInstanceAction action : instance.actions) {
      Action tmp = new Action();
      tmp.id = action.id;
      tmp.nextState = action.state;
      tmp.nextStateText = action.stateText;
      tmp.nextActivation = action.nextActivation;
      tmp.created = action.created;
      resp.actions.add(tmp);
    }
  }


}
