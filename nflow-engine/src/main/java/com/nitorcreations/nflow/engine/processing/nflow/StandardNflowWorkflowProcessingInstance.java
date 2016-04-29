package com.nitorcreations.nflow.engine.processing.nflow;

import com.nitorcreations.nflow.engine.internal.workflow.ObjectStringMapper;
import com.nitorcreations.nflow.engine.internal.workflow.StateExecutionImpl;
import com.nitorcreations.nflow.engine.internal.workflow.WorkflowStateMethod;
import com.nitorcreations.nflow.engine.processing.NextProcessingAction;
import com.nitorcreations.nflow.engine.processing.WorkflowProcessingDefinition;
import com.nitorcreations.nflow.engine.processing.WorkflowProcessingInstance;
import com.nitorcreations.nflow.engine.processing.WorkflowProcessingState;
import com.nitorcreations.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import org.joda.time.DateTime;

import static org.springframework.util.ReflectionUtils.invokeMethod;

public class StandardNflowWorkflowProcessingInstance implements WorkflowProcessingInstance {

  private final WorkflowInstance instance;
  private final AbstractWorkflowDefinition<? extends WorkflowState> definition;
  private final ObjectStringMapper objectMapper;
  private final WorkflowProcessingDefinition processingDefinition;

  public StandardNflowWorkflowProcessingInstance(WorkflowInstance instance,
                                                 WorkflowProcessingDefinition processingDefinition,
                                                 AbstractWorkflowDefinition<? extends WorkflowState> definition,
                                                 ObjectStringMapper objectMapper) {
    this.instance = instance;
    this.definition = definition;
    this.objectMapper = objectMapper;
    this.processingDefinition = processingDefinition;
  }

  @Override
  public WorkflowProcessingDefinition getWorkflowDefinition() {
    return processingDefinition;
  }

  @Override
  public WorkflowProcessingState getCurrentState() {
    return getWorkflowProcessingState(instance.state);
  }

  @Override
  public NextProcessingAction executeState(StateExecution stateExecution) {
    WorkflowStateMethod method = definition.getMethod(instance.state);
    // TODO ugly cast for StateExecutionImpl
    Object[] args = objectMapper.createArguments((StateExecutionImpl)stateExecution, method);
    NextAction nextAction = (NextAction) invokeMethod(method.method, definition, args);
    // TODO handle changes to StateVars
    WorkflowProcessingState state = getWorkflowProcessingState(nextAction.getNextState().name());

    // TODO handle exceptions etc
    // TODO ugly cast for StateExecutionImpl
    objectMapper.storeArguments((StateExecutionImpl)stateExecution, method, args);

    return NextProcessingAction.moveToStateAfter(state, nextAction.getActivation(), nextAction.getReason());
  }

  private WorkflowProcessingState getWorkflowProcessingState(String name) {
    // TODO maybe WorkflowProcessingDefinition.getState(name) ???
    for(WorkflowProcessingState state : getWorkflowDefinition().getStates()) {
      if(state.getName().equals(name)) {
        return state;
      }
    }
    throw new IllegalArgumentException("unknown state " + name);
  }

  @Override
  public int getRetryCount() {
    return instance.retries;
  }

  @Override
  public DateTime nextRetryTime() {
    // use default algo
    return null;
  }
}
