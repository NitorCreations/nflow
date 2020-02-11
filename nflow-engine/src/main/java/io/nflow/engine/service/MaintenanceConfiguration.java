package io.nflow.engine.service;

import org.joda.time.ReadablePeriod;
import org.springframework.util.Assert;

public class MaintenanceConfiguration {

  public final ConfigurationItem deleteArchivedWorkflows;
  public final ConfigurationItem archiveWorkflows;
  public final ConfigurationItem deleteWorkflows;
  public final ConfigurationItem deleteStates;

  MaintenanceConfiguration(ConfigurationItem deleteArchivedWorkflows, ConfigurationItem archiveWorkflows,
      ConfigurationItem deleteWorkflows, ConfigurationItem deleteStates) {
    this.deleteArchivedWorkflows = deleteArchivedWorkflows;
    this.archiveWorkflows = archiveWorkflows;
    this.deleteWorkflows = deleteWorkflows;
    this.deleteStates = deleteStates;
  }

  public static class Builder {

    private ConfigurationItem deleteArchivedWorkflows;
    private ConfigurationItem archiveWorkflows;
    private ConfigurationItem deleteWorkflows;
    private ConfigurationItem deleteStates;

    public MaintenanceConfiguration.Builder setDeleteArchivedWorkflows(ConfigurationItem deleteArchivedWorkflows) {
      this.deleteArchivedWorkflows = deleteArchivedWorkflows;
      return this;
    }

    public MaintenanceConfiguration.Builder setArchiveWorkflows(ConfigurationItem archiveWorkflows) {
      this.archiveWorkflows = archiveWorkflows;
      return this;
    }

    public MaintenanceConfiguration.Builder setDeleteWorkflows(ConfigurationItem deleteWorkflows) {
      this.deleteWorkflows = deleteWorkflows;
      return this;
    }

    public MaintenanceConfiguration.Builder setDeleteStates(ConfigurationItem deleteStates) {
      this.deleteStates = deleteStates;
      return this;
    }

    public MaintenanceConfiguration build() {
      return new MaintenanceConfiguration(deleteArchivedWorkflows, archiveWorkflows, deleteWorkflows, deleteStates);
    }
  }

  public static class ConfigurationItem {

    public final ReadablePeriod olderThanPeriod;
    public final int batchSize;

    public ConfigurationItem(ReadablePeriod olderThanPeriod, Integer batchSize) {
      this.olderThanPeriod = olderThanPeriod;
      this.batchSize = batchSize;
    }

    public static class Builder {

      private ReadablePeriod olderThanPeriod;
      private Integer batchSize = 1000;

      public Builder setOlderThanPeriod(ReadablePeriod olderThanPeriod) {
        this.olderThanPeriod = olderThanPeriod;
        return this;
      }

      /**
       * @param batchSize
       *          Number of workflows to operate on in single transaction. Typical value is 100-1000. This parameter mostly
       *          affects on performance.
       */
      public Builder setBatchSize(int batchSize) {
        this.batchSize = batchSize;
        return this;
      }

      public ConfigurationItem build() {
        Assert.isTrue(olderThanPeriod != null, "olderThanPeriod must not be null");
        Assert.isTrue(batchSize > 0, "batchSize must be greater than 0");
        return new ConfigurationItem(olderThanPeriod, batchSize);
      }
    }
  }
}
