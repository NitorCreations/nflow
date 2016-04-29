package com.nitorcreations.nflow.engine.processing.nflow;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.processing.AbstractWorkflowProcessingFactory;
import com.nitorcreations.nflow.engine.processing.WorkflowProcessingDefinition;
import com.nitorcreations.nflow.engine.processing.WorkflowProcessingInstance;
import com.nitorcreations.nflow.engine.service.WorkflowDefinitionService;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

public class StandardNflowWorkflowProcessingFactory extends AbstractWorkflowProcessingFactory {
  private final WorkflowDefinitionService workflowDefinitions;
  private final ObjectStringMapper objectMapper;

  public StandardNflowWorkflowProcessingFactory(WorkflowDefinitionService workflowDefinitions, ObjectStringMapper objectMapper) {
    this.workflowDefinitions = workflowDefinitions;
    this.objectMapper = objectMapper;
  }

  @Override
  public WorkflowProcessingInstance createInstance(WorkflowInstance instance) {
    AbstractWorkflowDefinition<? extends WorkflowState> definition = workflowDefinitions.getWorkflowDefinition(instance.type);
    WorkflowProcessingDefinition processDefinition = new StandardNfloWorkflowProcessingDefinition();
    return new StandardNflowWorkflowProcessingInstance(instance, processDefinition, definition, objectMapper);
  }

  @Override
  public boolean appliesTo(WorkflowInstance instance) {
    AbstractWorkflowDefinition<? extends WorkflowState> definition = workflowDefinitions.getWorkflowDefinition(instance.type);
    return definition != null;
  }
}
