package io.nflow.engine.service;

public class MaintenanceResults {

  public final int archivedWorkflows;
  public final int deletedArchivedWorkflows;
  public final int deletedWorkflows;
  public final int deletedStates;

  MaintenanceResults(int archivedWorkflows, int deletedArchivedWorkflows, int deletedWorkflows, int deletedStates) {
    this.archivedWorkflows = archivedWorkflows;
    this.deletedArchivedWorkflows = deletedArchivedWorkflows;
    this.deletedWorkflows = deletedWorkflows;
    this.deletedStates = deletedStates;
  }

  public static class Builder {

    private int archivedWorkflows;
    private int deletedArchivedWorkflows;
    private int deletedWorkflows;
    private int deletedStates;

    public Builder setArchivedWorkflows(int archivedWorkflows) {
      this.archivedWorkflows = archivedWorkflows;
      return this;
    }

    public Builder setDeletedArchivedWorkflows(int deletedArchivedWorkflows) {
      this.deletedArchivedWorkflows = deletedArchivedWorkflows;
      return this;
    }

    public Builder setDeletedWorkflows(int deletedWorkflows) {
      this.deletedWorkflows = deletedWorkflows;
      return this;
    }

    public Builder setDeletedStates(int deletedStates) {
      this.deletedStates = deletedStates;
      return this;
    }

    public MaintenanceResults build() {
      return new MaintenanceResults(archivedWorkflows, deletedArchivedWorkflows, deletedWorkflows, deletedStates);
    }
  }
}
