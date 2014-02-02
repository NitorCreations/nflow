package com.nitorcreations.nflow.rest.v0.converter;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceResponse;

@Component
public class CreateWorkflowConverter {

  public WorkflowInstance convertAndValidate(
      CreateWorkflowInstanceRequest req) {
    return new WorkflowInstance.Builder()
      .setType(req.type)
      .setBusinessKey(req.businessKey)
      .setNextActivation(req.activationTime)
      .setRequestData(req.requestData)
      .build();    
  }

  public CreateWorkflowInstanceResponse convert(
      int id, WorkflowInstance instance) {
    CreateWorkflowInstanceResponse resp = new CreateWorkflowInstanceResponse();
    resp.id = id;
    resp.type = instance.type;
    resp.businessKey = instance.businessKey;
    return resp;
  }

}
