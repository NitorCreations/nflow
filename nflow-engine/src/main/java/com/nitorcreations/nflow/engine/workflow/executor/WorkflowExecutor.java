package com.nitorcreations.nflow.engine.workflow.executor;

import org.joda.time.DateTime;

public class WorkflowExecutor {
  public final int id;
  public final String host;
  public final int pid;
  public final String executorGroup;
  public final DateTime started;
  public final DateTime active;
  public final DateTime expires;

  public WorkflowExecutor(int id, String host, int pid, String executorGroup, DateTime started, DateTime active, DateTime expires) {
    this.id = id;
    this.host = host;
    this.pid = pid;
    this.executorGroup = executorGroup;
    this.started = started;
    this.active = active;
    this.expires = expires;
  }
}
