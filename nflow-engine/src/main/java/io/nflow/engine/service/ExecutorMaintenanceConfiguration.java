package io.nflow.engine.service;

import org.joda.time.ReadablePeriod;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration for executor maintenance.
 */
public class ExecutorMaintenanceConfiguration {

  /**
   * Executors that have expired before (now - period) are removed.
   */
  public final ReadablePeriod deleteExpiredAfter;

  public ExecutorMaintenanceConfiguration(@JsonProperty("deleteExpiredAfter") ReadablePeriod deleteExpiredAfter) {
    this.deleteExpiredAfter = deleteExpiredAfter;
  }
}
