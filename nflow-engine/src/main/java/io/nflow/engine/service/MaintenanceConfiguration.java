package io.nflow.engine.service;

import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joda.time.ReadablePeriod;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonProperty;

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

  MaintenanceConfiguration(@JsonProperty("deleteArchivedWorkflows") ConfigurationItem deleteArchivedWorkflows,
                           @JsonProperty("archiveWorkflows") ConfigurationItem archiveWorkflows,
                           @JsonProperty("deleteWorkflows") ConfigurationItem deleteWorkflows) {
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
     * Default archive item limit
     */
    @JsonIgnore
    public static final int ARCHIVE_ITEM_LIMIT_UNLIMITED = -1;

    /**
     * Default batch size
     */
    @JsonIgnore
    public static final int BATCH_SIZE_NORMAL = 1000;

    /**
     * Items older than (now - period) are processed.
     */
    public final ReadablePeriod olderThanPeriod;

    /**
     * The batch size of the maintenance operation.
     */
    public final int batchSize;

    /**
     * Maximum amount of items to be archived per batch. Items exceeding the limit will not be archived, but will be deleted.
     */
    public final int archiveItemLimit;

    /**
     * The workflow types to be processed. If empty, process all types.
     */
    public final Set<String> workflowTypes;

    ConfigurationItem(@JsonProperty("olderThanPeriod") ReadablePeriod olderThanPeriod,
                      @JsonProperty("batchSize") Integer batchSize,
                      @JsonProperty("workflowTypes") Set<String> workflowTypes,
                      @JsonProperty("archiveItemLimit") Integer archiveItemLimit) {
      this.olderThanPeriod = olderThanPeriod;
      this.batchSize = batchSize;
      this.workflowTypes = ofNullable(workflowTypes).orElse(emptySet());
      this.archiveItemLimit = ofNullable(archiveItemLimit).orElse(ARCHIVE_ITEM_LIMIT_UNLIMITED);
    }

    /**
     * Builds ConfigurationItem objects.
     */
    public static class Builder {

      private final MaintenanceConfiguration.Builder parentBuilder;
      private ReadablePeriod olderThanPeriod;
      private Integer batchSize = BATCH_SIZE_NORMAL;
      private Set<String> workflowTypes = emptySet();
      private Integer archiveItemLimit = ARCHIVE_ITEM_LIMIT_UNLIMITED;

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
       * Set the batch size for the maintenance operation. Default is {@value #BATCH_SIZE_NORMAL}.
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

      /**
       * Set the maximum number of items to archive per batch. Default is {@value #ARCHIVE_ITEM_LIMIT_UNLIMITED}, which means unlimited.
       *
       * @param archiveItemLimit
       *          Maximum number of items, e.g. workflow states, that will be archived per batch. Generally unlimited is
       *          fine, but e.g. Galera cluster has a permitted maximum number of rows per write set.
       * @return this
       */
      public Builder setArchiveItemLimit(Integer archiveItemLimit) {
        this.archiveItemLimit = archiveItemLimit;
        return this;
      }

      /**
       * Finish ConfigurationItem object and move back to MaintenanceConfiguration.
       *
       * @return The parent MaintenanceConfiguration builder object.
       */
      public MaintenanceConfiguration.Builder done() {
        return parentBuilder;
      }

      ConfigurationItem build() {
        Assert.isTrue(olderThanPeriod != null, "olderThanPeriod must not be null");
        Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");
        Assert.isTrue(workflowTypes != null, "workflowTypes must not be null");
        Assert.isTrue(archiveItemLimit != null, "archiveItemLimit must not be null");
        return new ConfigurationItem(olderThanPeriod, batchSize, workflowTypes, archiveItemLimit);
      }
    }
  }
}
