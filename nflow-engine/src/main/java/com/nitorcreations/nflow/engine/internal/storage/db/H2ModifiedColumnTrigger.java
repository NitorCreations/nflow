package com.nitorcreations.nflow.engine.internal.storage.db;

import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

import org.h2.tools.TriggerAdapter;

public class H2ModifiedColumnTrigger extends TriggerAdapter {
  @Override
  public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
    Timestamp oldModified = oldRow.getTimestamp("modified");
    Timestamp newModified = newRow.getTimestamp("modified");
    if (Objects.equals(oldModified, newModified)) {
      newRow.updateTimestamp("modified", new Timestamp(currentTimeMillis()));
    }
  }
}
