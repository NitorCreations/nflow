package io.nflow.engine.internal.dao;

public enum NflowTables {
  MAIN("nflow_"),
  ARCHIVE("nflow_archive_");

  public final String workflow;
  public final String workflow_state;
  public final String workflow_action;
  public final String prefix;

  NflowTables(String prefix) {
    this.prefix = prefix;
    this.workflow = prefix + "workflow";
    this.workflow_state = prefix + "workflow_state";
    this.workflow_action = prefix + "workflow_action";
  }

  public static String asArchiveTable(String mainTable) {
    return ARCHIVE.prefix + mainTable.substring(MAIN.prefix.length());
  }

  public String replaceAll(String sql, NflowTables with) {
    // it is currently enough to change the prefix only since all tables other tables have just same suffix
    return sql.replace(this.workflow, with.workflow);
  }
}
