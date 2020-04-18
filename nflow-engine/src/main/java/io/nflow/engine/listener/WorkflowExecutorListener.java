package io.nflow.engine.listener;

import static org.joda.time.DateTime.now;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.model.ModelObject;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.instance.WorkflowInstance;

/**
 * WorkflowExecutorListener is a global, stateless listener for workflow
 * executors. The interface contains default (no-op) method implementations.
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
  public class ListenerContext extends ModelObject {

    /**
     * The time when the listener context was created.
     */
    public final DateTime start = now();

    /**
     * The definition of the workflow.
     */
    public final AbstractWorkflowDefinition<?> definition;

    /**
     * The name of the state of the workflow instance before processing.
     */
    public final String originalState;

    /**
     * The workflow instance.
     */
    public final WorkflowInstance instance;

    /**
     * The access point for the workflow instance-specific information.
     */
    public final StateExecution stateExecution;

    /**
     * The action to be taken after workflow state execution. Available in
     * afterProcessing and afterFailure stages only. Changing the value in the
     * listener has no effect.
     */
    public NextAction nextAction = null;

    /**
     * Stateless listeners can use data to pass information between listener
     * stages.
     */
    public final Map<Object, Object> data = new LinkedHashMap<>();

    public ListenerContext(AbstractWorkflowDefinition<?> definition, WorkflowInstance instance, StateExecution stateExecution) {
      this.definition = definition;
      this.instance = instance;
      this.stateExecution = stateExecution;
      this.originalState = instance.state;
    }
  }

  /**
   * Executed before state is processed. Exceptions are logged but they do not
   * affect workflow processing.
   * @param listenerContext The listener context.
   */
  default void beforeProcessing(ListenerContext listenerContext) {
    // no-op
  }

  /**
   * Processing chain.
   * Process methods in listeners form a
   * <a href="http://en.wikipedia.org/wiki/Chain-of-responsibility_pattern">Chain of Responsibility pattern</a>.
   * Listener can either call chain.next(listenerContext) to proceed to next filter or not to call it causing
   * processing of state to be skipped. Changes to workflowInstance (not child workflows or state variables) by
   * the filter will be persisted to database.
   * <p>
   *   Typical implementation:
   * </p>
   * <code>
   *   public NextAction process(ListenerContext listenerContext, ListenerChain chain) {
   *     return chain.next(listenerContext);
   *   }
   * </code>
   *
   * @param listenerContext The listener context.
   * @param chain The listener chain.
   * @return NextAction
   */
  default NextAction process(ListenerContext listenerContext, ListenerChain chain) {
    return chain.next(listenerContext);
  }

  /**
   * Executed after state has been successfully processed and before persisting
   * state. Exceptions are logged but they do not affect workflow processing.
   * @param listenerContext The listener context.
   */
  default void afterProcessing(ListenerContext listenerContext) {
    // no-op
  }

  /**
   * Executed after state processing has failed and before persisting state.
   * Exceptions are logged but they do not affect workflow processing.
   * @param listenerContext The listener context.
   * @param throwable The exception thrown by the state handler method.
   */
  default void afterFailure(ListenerContext listenerContext, Throwable throwable) {
    // no-op
  }

  /**
   * Called when instance processing is potentially stuck. Return true to interrupt the processing thread. Default implementation
   * returns false.
   * @param listenerContext The listener context.
   * @param processingTime How long the instances has been processed.
   * @return True if processing should be interruped, false otherwise.
   */
  default boolean handlePotentiallyStuck(ListenerContext listenerContext, Duration processingTime) {
    return false;
  }
}
