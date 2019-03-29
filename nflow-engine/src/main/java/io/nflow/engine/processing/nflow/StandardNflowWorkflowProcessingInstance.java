package io.nflow.engine.processing.nflow;

import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.internal.workflow.StateExecutionImpl;
import io.nflow.engine.internal.workflow.WorkflowStateMethod;
import io.nflow.engine.processing.NextProcessingAction;
import io.nflow.engine.processing.WorkflowProcessingDefinition;
import io.nflow.engine.processing.WorkflowProcessingInstance;
import io.nflow.engine.processing.WorkflowProcessingState;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.instance.WorkflowInstance;
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
    return processingDefinition.getState(instance.state);
  }

  @Override
  public NextProcessingAction executeState(StateExecution stateExecution) {
    WorkflowStateMethod method = definition.getMethod(instance.state);
    // TODO ugly cast for StateExecutionImpl
    Object[] args = objectMapper.createArguments((StateExecutionImpl)stateExecution, method);
    NextAction nextAction = (NextAction) invokeMethod(method.method, definition, args);
    // TODO handle changes to StateVars
    WorkflowProcessingState state = processingDefinition.getState(nextAction.getNextState().name());

    // TODO handle exceptions etc
    // TODO ugly cast for StateExecutionImpl
    objectMapper.storeArguments((StateExecutionImpl)stateExecution, method, args);

    return NextProcessingAction.moveToStateAfter(state, nextAction.getActivation(), nextAction.getReason());
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
