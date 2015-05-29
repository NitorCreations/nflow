package com.nitorcreations.nflow.engine.listener;

public interface ListenerChain {
  void next(WorkflowExecutorListener.ListenerContext context);
}