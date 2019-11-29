package io.nflow.engine.internal.workflow;

import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static java.util.UUID.randomUUID;
import static org.springframework.util.StringUtils.isEmpty;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.instance.WorkflowInstance;

@Component
public class WorkflowInstancePreProcessor {

  private final WorkflowDefinitionService workflowDefinitionService;

  private final WorkflowInstanceDao workflowInstanceDao;

  @Inject
  public WorkflowInstancePreProcessor(WorkflowDefinitionService workflowDefinitionService,
      WorkflowInstanceDao workflowInstanceDao) {
    this.workflowDefinitionService = workflowDefinitionService;
    this.workflowInstanceDao = workflowInstanceDao;
  }

  // TODO should this set next_activation for child workflows?
  public WorkflowInstance process(WorkflowInstance instance) {
    AbstractWorkflowDefinition<?> def = workflowDefinitionService.getWorkflowDefinition(instance.type);
    if (def == null) {
      throw new RuntimeException("No workflow definition found for type [" + instance.type + "]");
    }
    WorkflowInstance.Builder builder = new WorkflowInstance.Builder(instance);
    if (instance.state == null) {
      builder.setState(def.getInitialState().name());
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
    if (instance.priority == null) {
      builder.setPriority(def.getSettings().getDefaultPriority());
    }
    instance.getChangedStateVariables().forEach(workflowInstanceDao::checkStateVariableValue);
    return builder.build();
  }
}
