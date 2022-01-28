package io.nflow.engine.internal.dao;

import io.nflow.engine.workflow.instance.WorkflowInstance;

public enum NflowTable {
  WORKFLOW("workflow"),
  STATE("workflow_state"),
  ACTION("workflow_action");

  public String main;
  public String archive;

  NflowTable(String table) {
    this.main = TableType.MAIN.prefix + table;
    this.archive = TableType.ARCHIVE.prefix + table;
  }

  public String tableFor(TableType type) {
    return type == TableType.MAIN ? main : archive;
  }

  public String tableFor(WorkflowInstance instance) {
    return instance.isArchived ? archive : main;
  }
}
