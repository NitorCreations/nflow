package com.nitorcreations.nflow.engine.dao;

import static java.lang.System.currentTimeMillis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.h2.tools.TriggerAdapter;

public class H2ModifiedColumnTrigger extends TriggerAdapter {
  static ThreadLocal<Boolean> modifiedTriggerActive = new ThreadLocal<Boolean>() {
    @Override
    protected Boolean initialValue() {
      return false;
    }
  };

  @Override
  public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
    if (modifiedTriggerActive.get()) {
      return;
    }
    modifiedTriggerActive.set(true);
    try (PreparedStatement prep = conn.prepareStatement("update " + tableName + " set modified = ? where id = ?")) {
      prep.setTimestamp(1, new Timestamp(currentTimeMillis()));
      prep.setLong(2, newRow.getLong("id"));
      prep.execute();
    } finally {
      modifiedTriggerActive.set(false);
    }
  }
}