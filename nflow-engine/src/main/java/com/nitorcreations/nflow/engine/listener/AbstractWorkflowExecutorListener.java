package com.nitorcreations.nflow.engine.listener;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;

/**
 * AbstractWorkflowExecutorListener implement WorkflowExecutorListener with no-op/defaults
 * method implementations. A subclasses can override just the methods it is interested in.
 */
abstract public class AbstractWorkflowExecutorListener implements WorkflowExecutorListener {

  /**
   * Does nothing.
   * @param listenerContext The listener context.
   */
  @Override
  public void beforeProcessing(ListenerContext listenerContext) {
    // no-op
  }

  /**
   * Calls next listener in chain.
   * @param listenerContext The listener context.
   * @param chain The listener chain.
   * @return The next action to be taken in the workflow.
   */
  @Override
  public NextAction process(ListenerContext listenerContext, ListenerChain chain) {
    return chain.next(listenerContext);
  }

  /**
   * Does nothing.
   * @param listenerContext The listener context.
   */
  @Override
  public void afterProcessing(ListenerContext listenerContext) {
    // no-op
  }

  /**
   * Does nothing.
   * @param listenerContext The listener context.
   * @param throwable The throwable that was thrown during state processing.
   */
  @Override
  public void afterFailure(ListenerContext listenerContext, Throwable throwable) {
    // no-op
  }
}
