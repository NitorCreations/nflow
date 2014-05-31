package com.nitorcreations.nflow.engine;

import static org.joda.time.DateTime.now;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public interface WorkflowExecutorListener {

  @SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification="listeners are implemented by business applications")
  class ListenerContext {
    public final DateTime start = now();
    public final WorkflowDefinition<?> definition;
    public final String originalState;
    public final WorkflowInstance instance;
    public final StateExecution stateExecution;
    /** Stateless listeners can use data to pass information between listener stages. */
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
