package com.nitorcreations.nflow.engine.listener;

import static org.joda.time.DateTime.now;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * WorkflowExecutorListener is a global, stateless listener for workflow
 * executors.
 * <p>
 * Same instance of WorkflowExecutorListener is used for all workflow
 * state executions: all state must be stored in <code>ListenerContext.data</code>.
 * </p>
 */
public interface WorkflowExecutorListener {

  /**
   * ListenerContext instance is created at start of workflow state execution and passed to listener's
   * life-cycle methods.
   */
  @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "listeners are implemented by business applications")
  public class ListenerContext {
    public final DateTime start = now();
    public final WorkflowDefinition<?> definition;
    public final String originalState;
    public final WorkflowInstance instance;
    public final StateExecution stateExecution;
    public NextAction nextAction = null;
    /**
     * Stateless listeners can use data to pass information between listener
     * stages.
     */
    public final Map<Object, Object> data = new LinkedHashMap<>();

    public ListenerContext(WorkflowDefinition<?> definition,
        WorkflowInstance instance, StateExecution stateExecution) {
      this.definition = definition;
      this.instance = instance;
      this.stateExecution = stateExecution;
      this.originalState = instance.state;
    }
  }

  /** Executed before state is processed. */
  void beforeProcessing(ListenerContext listenerContext);

  /**
   * Executed after state has been successfully processed and before persisting
   * state.
   */
  void afterProcessing(ListenerContext listenerContext);

  /** Executed after state processing has failed and before persisting state. */
  void afterFailure(ListenerContext listenerContext, Throwable exeption);
}
