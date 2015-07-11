package com.nitorcreations.nflow.engine.internal.dao;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

public class DaoUtil {

  static final FirstColumnLengthExtractor firstColumnLengthExtractor = new FirstColumnLengthExtractor();

  private DaoUtil() {
    // prevent instantiation
  }

  public static Timestamp toTimestamp(DateTime time) {
    return time == null ? null : new Timestamp(time.getMillis());
  }

  public static DateTime toDateTime(Timestamp time) {
    return time == null ? null : new DateTime(time.getTime());
  }

  static final class FirstColumnLengthExtractor implements ResultSetExtractor<Integer> {
    @Override
    public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
      return rs.getMetaData().getColumnDisplaySize(1);
    }
  }

  public static Integer getInt(ResultSet rs, String columnLabel) throws SQLException {
    int value = rs.getInt(columnLabel);
    return rs.wasNull() ? null : value;
  }

  public static final class ColumnNamesExtractor implements org.springframework.jdbc.core.ResultSetExtractor<List<String>> {
    static final ColumnNamesExtractor columnNamesExtractor = new ColumnNamesExtractor();
    private ColumnNamesExtractor() {}

    @Override
    public List<String> extractData(ResultSet rs) throws SQLException, DataAccessException {
      List<String> columnNames = new LinkedList<>();

      ResultSetMetaData metadata = rs.getMetaData();
      for(int col = 1; col <= metadata.getColumnCount(); col ++) {
        columnNames.add(metadata.getColumnName(col));
      }
      return columnNames;
    }
  }

}
