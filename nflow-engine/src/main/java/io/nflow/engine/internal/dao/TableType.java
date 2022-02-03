package io.nflow.engine.internal.dao;

public enum TableType {
  MAIN("nflow_"), ARCHIVE("nflow_archive_");

  public final String prefix;

  TableType(String prefix) {
    this.prefix = prefix;
  }

  static String convertMainToArchive(String sql) {
    return sql.replaceAll(MAIN.prefix, ARCHIVE.prefix);
  }
}
