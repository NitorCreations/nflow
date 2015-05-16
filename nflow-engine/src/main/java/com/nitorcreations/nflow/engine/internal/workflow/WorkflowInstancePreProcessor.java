package com.nitorcreations.nflow.engine.internal.workflow;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static java.util.UUID.randomUUID;
import static org.springframework.util.StringUtils.isEmpty;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

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
    AbstractWorkflowDefinition<?> def = workflowDefinitionService.getWorkflowDefinition(instance.type);
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
      builder.setExternalId(randomUUID().toString());
    }
    if (instance.status == null) {
      builder.setStatus(created);
    }
    return builder.build();
  }
}
