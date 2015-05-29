package com.nitorcreations.nflow.engine.listener;

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
   */
  @Override
  public void process(ListenerContext listenerContext, ListenerChain chain) {
    chain.next(listenerContext);
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
   */
  @Override
  public void afterFailure(ListenerContext listenerContext, Throwable throwable) {
    // no-op
  }
}
