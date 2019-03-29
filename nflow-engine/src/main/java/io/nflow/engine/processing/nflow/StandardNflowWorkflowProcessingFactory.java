package io.nflow.engine.processing.nflow;

import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.processing.AbstractWorkflowProcessingFactory;
import io.nflow.engine.processing.WorkflowProcessingDefinition;
import io.nflow.engine.processing.WorkflowProcessingInstance;
import io.nflow.engine.processing.WorkflowProcessingSettings;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.instance.WorkflowInstance;

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
    WorkflowProcessingSettings settings = convertWorkflowSettings(definition.getSettings());
    WorkflowProcessingDefinition processDefinition = new StandardNfloWorkflowProcessingDefinition(definition, settings);
    return new StandardNflowWorkflowProcessingInstance(instance, processDefinition, definition, objectMapper);
  }

  @Override
  public boolean appliesTo(WorkflowInstance instance) {
    AbstractWorkflowDefinition<? extends WorkflowState> definition = workflowDefinitions.getWorkflowDefinition(instance.type);
    return definition != null;
  }

  private WorkflowProcessingSettings convertWorkflowSettings(WorkflowSettings settings) {
    // TODO
    return new WorkflowProcessingSettings() {};
  }
}
