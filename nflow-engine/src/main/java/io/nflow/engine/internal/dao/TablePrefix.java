package io.nflow.engine.internal.dao;

public enum TablePrefix {
  MAIN("nflow_"),
  ARCHIVE("nflow_archive_");

  private final String prefix;

  TablePrefix(String prefix) {
    this.prefix = prefix;
  }

  public String nameOf(String name) {
    return prefix + name;
  }
}
