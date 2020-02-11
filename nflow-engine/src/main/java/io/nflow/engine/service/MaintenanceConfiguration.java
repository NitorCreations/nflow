package io.nflow.engine.service;

import org.joda.time.ReadablePeriod;
import org.springframework.util.Assert;

/**
 * Configuration for all maintenance operations.
 */
public class MaintenanceConfiguration {

  /**
   * Configuration for archiving old workflow instances.
   */
  public final ConfigurationItem archiveWorkflows;

  /**
   * Configuration for deleting old workflow instances from archive tables.
   */
  public final ConfigurationItem deleteArchivedWorkflows;

  /**
   * Configuration for deleting old workflow instances from main tables.
   */
  public final ConfigurationItem deleteWorkflows;

  /**
   * Configuration for deleting old workflow state variables.
   */
  public final ConfigurationItem deleteStates;

  MaintenanceConfiguration(ConfigurationItem deleteArchivedWorkflows, ConfigurationItem archiveWorkflows,
      ConfigurationItem deleteWorkflows, ConfigurationItem deleteStates) {
    this.deleteArchivedWorkflows = deleteArchivedWorkflows;
    this.archiveWorkflows = archiveWorkflows;
    this.deleteWorkflows = deleteWorkflows;
    this.deleteStates = deleteStates;
  }

  /**
   * Builds MaintenanceConfiguration objects.
   */
  public static class Builder {

    private ConfigurationItem deleteArchivedWorkflows;
    private ConfigurationItem archiveWorkflows;
    private ConfigurationItem deleteWorkflows;
    private ConfigurationItem deleteStates;

    /**
     * Configuration for deleting old workflow instances from archive tables.
     *
     * @param archiveWorkflows
     *          Configuration item
     * @return this
     */
    public MaintenanceConfiguration.Builder setArchiveWorkflows(ConfigurationItem archiveWorkflows) {
      this.archiveWorkflows = archiveWorkflows;
      return this;
    }

    /**
     * Set configuration for deleting old workflow instances from archive tables.
     *
     * @param deleteArchivedWorkflows
     *          Configuration item
     * @return this
     */
    public MaintenanceConfiguration.Builder setDeleteArchivedWorkflows(ConfigurationItem deleteArchivedWorkflows) {
      this.deleteArchivedWorkflows = deleteArchivedWorkflows;
      return this;
    }

    /**
     * Set configuration for deleting old workflow instances from main tables.
     *
     * @param deleteWorkflows
     *          Configuration item
     * @return this
     */
    public MaintenanceConfiguration.Builder setDeleteWorkflows(ConfigurationItem deleteWorkflows) {
      this.deleteWorkflows = deleteWorkflows;
      return this;
    }

    /**
     * Set configuration for deleting old workflow states.
     *
     * @param deleteStates
     *          Configuration item
     * @return this
     */
    public MaintenanceConfiguration.Builder setDeleteStates(ConfigurationItem deleteStates) {
      this.deleteStates = deleteStates;
      return this;
    }

    /**
     * Build MaintenanceConfiguration object.
     *
     * @return MaintenanceConfiguration object.
     */
    public MaintenanceConfiguration build() {
      return new MaintenanceConfiguration(deleteArchivedWorkflows, archiveWorkflows, deleteWorkflows, deleteStates);
    }
  }

  /**
   * Configuration for a single maintenance operation.
   */
  public static class ConfigurationItem {

    /**
     * Items older than (now - period) are processed.
     */
    public final ReadablePeriod olderThanPeriod;

    /**
     * The batch size of the maintenance operation.
     */
    public final int batchSize;

    ConfigurationItem(ReadablePeriod olderThanPeriod, Integer batchSize) {
      this.olderThanPeriod = olderThanPeriod;
      this.batchSize = batchSize;
    }

    /**
     * Builds ConfigurationItem objects.
     */
    public static class Builder {

      private ReadablePeriod olderThanPeriod;
      private Integer batchSize = 1000;

      /**
       * Set the time limit for the maintenance operation. Items older than (now - period) are processed.
       *
       * @param olderThanPeriod
       *          Time limit
       * @return this
       */
      public Builder setOlderThanPeriod(ReadablePeriod olderThanPeriod) {
        this.olderThanPeriod = olderThanPeriod;
        return this;
      }

      /**
       * Set the batch size for the maintenance operation. Default is 1000.
       *
       * @param batchSize
       *          Number of workflows to operate on in single transaction. Typical value is 100-1000. This parameter mostly
       *          affects on performance.
       * @return this
       */
      public Builder setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
      }

      /**
       * Build ConfigurationItem object.
       *
       * @return ConfigurationItem object.
       */
      public ConfigurationItem build() {
        Assert.isTrue(olderThanPeriod != null, "olderThanPeriod must not be null");
        Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");
        return new ConfigurationItem(olderThanPeriod, batchSize);
      }
    }
  }
}
