package com.nitorcreations.nflow.engine.listener;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;

/**
 * The workflow executor listener chain.
 */
public interface ListenerChain {

  /**
   * Return the next listener in the chain.
   * @param context The workflow execution listener context.
   * @return The next action to be taken in the workflow processing.
   */
  NextAction next(WorkflowExecutorListener.ListenerContext context);
}
