package io.nflow.engine.internal.dao;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import io.nflow.engine.workflow.instance.WorkflowInstance;

class NflowTableTest {

  @Test
  public void returnsCorrectTableNamesForTableType() {
    assertThat(NflowTable.WORKFLOW.tableFor(TableType.MAIN), is("nflow_workflow"));
    assertThat(NflowTable.WORKFLOW.tableFor(TableType.ARCHIVE), is("nflow_archive_workflow"));
    assertThat(NflowTable.STATE.tableFor(TableType.MAIN), is("nflow_workflow_state"));
    assertThat(NflowTable.STATE.tableFor(TableType.ARCHIVE), is("nflow_archive_workflow_state"));
    assertThat(NflowTable.ACTION.tableFor(TableType.MAIN), is("nflow_workflow_action"));
    assertThat(NflowTable.ACTION.tableFor(TableType.ARCHIVE), is("nflow_archive_workflow_action"));
  }

  @Test
  public void returnsCorrectTableNamesForWorkflowInstance() {
    WorkflowInstance instance = new WorkflowInstance.Builder().build();
    WorkflowInstance archivedInstance = new WorkflowInstance.Builder().setArchived(true).build();
    assertThat(NflowTable.WORKFLOW.tableFor(instance), is("nflow_workflow"));
    assertThat(NflowTable.WORKFLOW.tableFor(archivedInstance), is("nflow_archive_workflow"));
    assertThat(NflowTable.STATE.tableFor(instance), is("nflow_workflow_state"));
    assertThat(NflowTable.STATE.tableFor(archivedInstance), is("nflow_archive_workflow_state"));
    assertThat(NflowTable.ACTION.tableFor(instance), is("nflow_workflow_action"));
    assertThat(NflowTable.ACTION.tableFor(archivedInstance), is("nflow_archive_workflow_action"));
  }

  @Test
  public void hasCorrectTableNamesAsFields() {
    assertThat(NflowTable.WORKFLOW.main, is("nflow_workflow"));
    assertThat(NflowTable.WORKFLOW.archive, is("nflow_archive_workflow"));
    assertThat(NflowTable.STATE.main, is("nflow_workflow_state"));
    assertThat(NflowTable.STATE.archive, is("nflow_archive_workflow_state"));
    assertThat(NflowTable.ACTION.main, is("nflow_workflow_action"));
    assertThat(NflowTable.ACTION.archive, is("nflow_archive_workflow_action"));
  }
}
