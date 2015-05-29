package com.nitorcreations.nflow.engine.listener;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;

public interface ListenerChain {
  NextAction next(WorkflowExecutorListener.ListenerContext context);
}