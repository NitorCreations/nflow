package com.nitorcreations.nflow.engine.internal.dao;

import java.sql.Timestamp;

import org.joda.time.DateTime;

public class DaoUtil {

  private DaoUtil() {
    // prevent instantiation
  }

  public static Timestamp toTimestamp(DateTime time) {
    return time == null ? null : new Timestamp(time.getMillis());
  }

  public static DateTime toDateTime(Timestamp time) {
    return time == null ? null : new DateTime(time.getTime());
  }
}
