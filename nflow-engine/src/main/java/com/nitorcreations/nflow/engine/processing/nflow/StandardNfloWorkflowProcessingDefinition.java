package com.nitorcreations.nflow.engine.processing.nflow;

import com.nitorcreations.nflow.engine.processing.WorkflowProcessingDefinition;
import com.nitorcreations.nflow.engine.processing.WorkflowProcessingState;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;

import java.util.Collections;
import java.util.List;

public class StandardNfloWorkflowProcessingDefinition implements WorkflowProcessingDefinition
{
  private final AbstractWorkflowDefinition<? extends WorkflowState> definition;
  public StandardNfloWorkflowProcessingDefinition(AbstractWorkflowDefinition<? extends WorkflowState> definition) {
    this.definition = definition;
  }


  @Override
  public String getName() {
    return definition.getName();
  }

  @Override
  public String getDescription() {
    return definition.getDescription();
  }

  @Override
  public WorkflowProcessingState getGenericErrorState() {
    // TODO
    return null;
  }

  @Override
  public List<WorkflowProcessingState> getStates() {
    // TODO loop over definition.getStates() and create WorkflowProcessingState objects
    return Collections.emptyList();
  }
}
