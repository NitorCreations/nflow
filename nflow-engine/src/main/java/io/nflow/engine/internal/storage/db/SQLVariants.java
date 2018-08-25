package io.nflow.engine.internal.storage.db;

import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import org.joda.time.DateTime;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static io.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static io.nflow.engine.internal.dao.DaoUtil.toTimestamp;

public interface SQLVariants {
  String currentTimePlusSeconds(int seconds);

  boolean hasUpdateReturning();

  String workflowStatus(WorkflowInstanceStatus status);

  String workflowStatus();

  String actionType();

  boolean hasUpdateableCTE();

  String nextActivationUpdate();

  String castToText();

  String limit(String query, String limit);

  int longTextType();

  boolean useBatchUpdate();

  default String dateLtEqDiff(String next_activation, String current_timestamp) {
    return next_activation + " <= " + current_timestamp;
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
}
