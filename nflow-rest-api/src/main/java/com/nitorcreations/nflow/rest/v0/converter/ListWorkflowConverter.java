package com.nitorcreations.nflow.rest.v0.converter;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse;

@Component
public class ListWorkflowConverter {

  public ListWorkflowInstanceResponse convert(
      WorkflowInstance instance) {
    ListWorkflowInstanceResponse resp = new ListWorkflowInstanceResponse();
    resp.id = instance.id;
    resp.type = instance.type;
    resp.businessKey = instance.businessKey;
    resp.state = instance.state;
    resp.stateText = instance.stateText;
    resp.nextActivation = instance.nextActivation;
    return resp;
  }

}
