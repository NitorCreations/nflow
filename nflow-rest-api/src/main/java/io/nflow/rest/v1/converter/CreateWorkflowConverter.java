package io.nflow.rest.v1.converter;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Map.Entry;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;

@Component
public class CreateWorkflowConverter {
  private final WorkflowInstanceFactory factory;

  @Inject
  public CreateWorkflowConverter(WorkflowInstanceFactory factory) {
    this.factory = factory;
  }

  public WorkflowInstance convert(CreateWorkflowInstanceRequest req) {
    WorkflowInstance.Builder builder = factory.newWorkflowInstanceBuilder().setType(req.type).setBusinessKey(req.businessKey)
        .setExternalId(req.externalId);
    if (req.activationTime != null) {
      builder.setNextActivation(req.activationTime);
    }
    if (isNotEmpty(req.startState)) {
      builder.setState(req.startState);
    }
    for (Entry<String, Object> entry : req.stateVariables.entrySet()) {
      Object value = entry.getValue();
      if (value instanceof String) {
        builder.putStateVariable(entry.getKey(), (String) value);
      } else {
        builder.putStateVariable(entry.getKey(), value);
      }
    }
    return builder.build();
  }

  public CreateWorkflowInstanceResponse convert(WorkflowInstance instance) {
    CreateWorkflowInstanceResponse resp = new CreateWorkflowInstanceResponse();
    resp.id = instance.id;
    resp.type = instance.type;
    resp.businessKey = instance.businessKey;
    resp.externalId = instance.externalId;
    return resp;
  }

}
