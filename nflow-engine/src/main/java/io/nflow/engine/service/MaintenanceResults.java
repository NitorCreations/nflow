package io.nflow.engine.service;

/**
 * Represent maintenance operation results.
 */
public class MaintenanceResults {

  /**
   * Number of workflows archived.
   */
  public final int archivedWorkflows;

  /**
   * Number of workflows deleted from archive table.
   */
  public final int deletedArchivedWorkflows;

  /**
   * Number of workflows deleted from main table.
   */
  public final int deletedWorkflows;

  /**
   * Number of workflow states deleted.
   */
  public final int deletedStates;

  MaintenanceResults(int archivedWorkflows, int deletedArchivedWorkflows, int deletedWorkflows, int deletedStates) {
    this.archivedWorkflows = archivedWorkflows;
    this.deletedArchivedWorkflows = deletedArchivedWorkflows;
    this.deletedWorkflows = deletedWorkflows;
    this.deletedStates = deletedStates;
  }

  /**
   * Build MaintenanceResults objects.
   */
  public static class Builder {

    private int archivedWorkflows;
    private int deletedArchivedWorkflows;
    private int deletedWorkflows;
    private int deletedStates;

    /**
     * Set number of workflows archived.
     *
     * @param archivedWorkflows
     *          Number of workflows archived.
     * @return this
     */
    public Builder setArchivedWorkflows(int archivedWorkflows) {
      this.archivedWorkflows = archivedWorkflows;
      return this;
    }

    /**
     * Set number of archived workflows deleted.
     *
     * @param deletedArchivedWorkflows
     *          Number of archived workflows deleted.
     * @return this
     */
    public Builder setDeletedArchivedWorkflows(int deletedArchivedWorkflows) {
      this.deletedArchivedWorkflows = deletedArchivedWorkflows;
      return this;
    }

    /**
     * Set number of workflows deleted.
     *
     * @param deletedWorkflows
     *          Number of workflows deleted.
     * @return this
     */
    public Builder setDeletedWorkflows(int deletedWorkflows) {
      this.deletedWorkflows = deletedWorkflows;
      return this;
    }

    /**
     * Set number of workflow states deleted.
     *
     * @param deletedStates
     *          Number of workflow states deleted.
     * @return this
     */
    public Builder setDeletedStates(int deletedStates) {
      this.deletedStates = deletedStates;
      return this;
    }

    /**
     * Build MaintenanceResults object.
     *
     * @return MaintenanceResults object.
     */
    public MaintenanceResults build() {
      return new MaintenanceResults(archivedWorkflows, deletedArchivedWorkflows, deletedWorkflows, deletedStates);
    }
  }
}
