package io.nflow.engine.internal.storage.db;

import static io.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static io.nflow.engine.internal.dao.DaoUtil.toTimestamp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.joda.time.DateTime;

import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;

public interface SQLVariants {
  String currentTimePlusSeconds(int seconds);

  default boolean hasUpdateReturning() {
    return false;
  }

  String workflowStatus(WorkflowInstanceStatus status);

  String workflowStatus();

  String actionType();

  boolean hasUpdateableCTE();

  String nextActivationUpdate();

  String castToText();

  String limit(String query, long limit);

  int longTextType();

  boolean useBatchUpdate();

  default String forUpdateSkipLocked() {
    return " for update";
  }

  default String dateLtEqDiff(String date1, String date2) {
    return date1 + " <= " + date2;
  }

  default Object getTimestamp(ResultSet rs, String columnName) throws SQLException {
    return rs.getTimestamp(columnName);
  }

  default DateTime getDateTime(ResultSet rs, String columnName) throws SQLException {
    return toDateTime(rs.getTimestamp(columnName));
  }

  default void setDateTime(PreparedStatement ps, int columnNumber, DateTime timestamp) throws SQLException {
    ps.setTimestamp(columnNumber, toTimestamp(timestamp));
  }

  default Object toTimestampObject(DateTime timestamp) {
    return toTimestamp(timestamp);
  }

  default Object tuneTimestampForDb(Object timestamp) {
    return timestamp;
  }

  default String withUpdateSkipLocked() {
    return "";
  }

  default String caseSensitiveLike() { return "like"; }

  default String clobToComparable(String column) {
    return column;
  }
}
