package com.nitorcreations.nflow.engine.workflow.executor;

import org.joda.time.DateTime;

/**
 * Describes one workflow executor.
 */
public class WorkflowExecutor {
  /**
   * Unique identifier of executor instance. Each time an executor is started it receives a new identifier.
   */
  public final int id;
  /**
   * The host name of the executor.
   */
  public final String host;
  /**
   * Process id of the executor.
   */
  public final int pid;
  /**
   * The executor group of the executor.
   */
  public final String executorGroup;
  /**
   * Time when the executor was started.
   */
  public final DateTime started;
  /**
   * Time when the executor last updated that it is active.
   */
  public final DateTime active;
  /**
   * Time after which the executor is considered dead.
   */
  public final DateTime expires;

  /**
   * Creates a new workflow executor description.
   *
   * @param id Unique identifier of executor instance
   * @param host The host name of the executor
   * @param pid Process id of the executor
   * @param executorGroup The executor group of the executor
   * @param started Time when the executor was started
   * @param active Time when the executor last updated that it is active
   * @param expires Time after which the executor is considered dead
   */
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
