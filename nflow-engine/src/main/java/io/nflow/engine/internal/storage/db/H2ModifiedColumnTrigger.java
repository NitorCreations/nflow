package io.nflow.engine.internal.storage.db;

import static org.joda.time.DateTimeUtils.currentTimeMillis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.h2.tools.TriggerAdapter;

public class H2ModifiedColumnTrigger extends TriggerAdapter {
  @Override
  public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
    long oldModified = getMillis(oldRow.getObject("modified"));
    long newModified = getMillis(newRow.getObject("modified"));
    if (oldModified == newModified) {
      newRow.updateTimestamp("modified", new Timestamp(currentTimeMillis()));
    }
  }

  private long getMillis(Object h2Time) {
    if (h2Time instanceof Timestamp) {
      return ((Timestamp) h2Time).getTime();
    }
    if (h2Time instanceof OffsetDateTime) {
      return ((OffsetDateTime) h2Time).toInstant().toEpochMilli();
    }
    if (h2Time instanceof LocalDateTime) {
      return ((LocalDateTime) h2Time).toInstant(ZoneId.systemDefault().getRules().getOffset((LocalDateTime) h2Time))
          .toEpochMilli();
    }
    throw new UnsupportedOperationException("No support for converting " + h2Time.getClass() + " to milliseconds");
  }
}
