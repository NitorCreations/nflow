package io.nflow.engine.internal.dao;

public enum TablePrefix {
  MAIN("nflow_"),
  ARCHIVE("nflow_archive_");

  public final String workflow;
  public final String workflow_state;
  public final String workflow_action;
  public final String prefix;

  TablePrefix(String prefix) {
    this.prefix = prefix;
    this.workflow = prefix + "workflow";
    this.workflow_state = prefix + "workflow_state";
    this.workflow_action = prefix + "workflow_action";
  }

  public static String asArchiveTable(String mainTable) {
    return ARCHIVE.prefix + mainTable.substring(MAIN.prefix.length());
  }
}
