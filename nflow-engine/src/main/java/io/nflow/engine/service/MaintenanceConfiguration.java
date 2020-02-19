package io.nflow.engine.service;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

import java.util.Set;

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

  MaintenanceConfiguration(ConfigurationItem deleteArchivedWorkflows, ConfigurationItem archiveWorkflows,
      ConfigurationItem deleteWorkflows) {
    this.deleteArchivedWorkflows = deleteArchivedWorkflows;
    this.archiveWorkflows = archiveWorkflows;
    this.deleteWorkflows = deleteWorkflows;
  }

  /**
   * Builds MaintenanceConfiguration objects.
   */
  public static class Builder {

    private ConfigurationItem.Builder deleteArchivedWorkflows;
    private ConfigurationItem.Builder archiveWorkflows;
    private ConfigurationItem.Builder deleteWorkflows;

    /**
     * Configuration for deleting old workflow instances from archive tables.
     *
     * @return builder for configuration
     */
    public ConfigurationItem.Builder withArchiveWorkflows() {
      return archiveWorkflows = new ConfigurationItem.Builder(this);
    }

    /**
     * Set configuration for deleting old workflow instances from archive tables.
     *
     * @return builder for configuration
     */
    public ConfigurationItem.Builder withDeleteArchivedWorkflows() {
      return deleteArchivedWorkflows = new ConfigurationItem.Builder(this);
    }

    /**
     * Set configuration for deleting old workflow instances from main tables.
     *
     * @return builder for configuration
     */
    public ConfigurationItem.Builder withDeleteWorkflows() {
      return deleteWorkflows = new ConfigurationItem.Builder(this);
    }

    private ConfigurationItem build(ConfigurationItem.Builder builder) {
      return ofNullable(builder).map(ConfigurationItem.Builder::build).orElse(null);
    }

    /**
     * Build MaintenanceConfiguration object.
     *
     * @return MaintenanceConfiguration object.
     */
    public MaintenanceConfiguration build() {
      return new MaintenanceConfiguration(build(deleteArchivedWorkflows), build(archiveWorkflows), build(deleteWorkflows));
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

    /**
     * The workflow types to be processed. If empty, process all types.
     */
    public final Set<String> workflowTypes;

    ConfigurationItem(ReadablePeriod olderThanPeriod, Integer batchSize, Set<String> workflowTypes) {
      this.olderThanPeriod = olderThanPeriod;
      this.batchSize = batchSize;
      this.workflowTypes = workflowTypes;
    }

    /**
     * Builds ConfigurationItem objects.
     */
    public static class Builder {

      private final MaintenanceConfiguration.Builder parentBuilder;
      private ReadablePeriod olderThanPeriod;
      private Integer batchSize = 1000;
      private Set<String> workflowTypes = emptySet();

      Builder(MaintenanceConfiguration.Builder parentBuilder) {
        this.parentBuilder = parentBuilder;
      }

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
       * Set the workflow types to be processed. Default is empty, which means all types are processed.
       *
       * @param workflowTypes
       *          Workflow types to be processed.
       * @return this
       */
      public Builder setWorkflowTypes(Set<String> workflowTypes) {
        this.workflowTypes = workflowTypes;
        return this;
      }

      public MaintenanceConfiguration.Builder done() {
        return parentBuilder;
      }

      /**
       * Build ConfigurationItem object.
       *
       * @return ConfigurationItem object.
       */
      ConfigurationItem build() {
        Assert.isTrue(olderThanPeriod != null, "olderThanPeriod must not be null");
        Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");
        Assert.isTrue(workflowTypes != null, "workflowTypes must not be null");
        return new ConfigurationItem(olderThanPeriod, batchSize, workflowTypes);
      }
    }
  }
}
