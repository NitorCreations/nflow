package com.nitorcreations.nflow.engine.internal.workflow;

import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.UUID;

import static org.springframework.util.StringUtils.isEmpty;

@Component
public class WorkflowInstancePreProcessor {

  @Inject
  private WorkflowDefinitionService workflowDefinitionService;

  public WorkflowInstancePreProcessor() {
  }

  public WorkflowInstancePreProcessor(WorkflowDefinitionService workflowDefinitionService) {
    this.workflowDefinitionService = workflowDefinitionService;
  }

  // TODO should this set next_activation for child workflows?
  public WorkflowInstance process(WorkflowInstance instance) {
    WorkflowDefinition<?> def = workflowDefinitionService.getWorkflowDefinition(instance.type);
    if (def == null) {
      throw new RuntimeException("No workflow definition found for type [" + instance.type + "]");
    }
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance);
    if (instance.state == null) {
      builder.setState(def.getInitialState().toString());
    } else {
      if (!def.isStartState(instance.state)) {
        throw new RuntimeException("Specified state [" + instance.state + "] is not a start state.");
      }
    }
    if (isEmpty(instance.externalId)) {
      builder.setExternalId(UUID.randomUUID().toString());
    }
    if (instance.status == null) {
      builder.setStatus(WorkflowInstance.WorkflowInstanceStatus.created);
    }
    return builder.build();
  }
}
