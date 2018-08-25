package io.nflow.engine.config;

/**
 * Constants for profile names supported by nFlow.
 */
public abstract class Profiles {

  /**
   * Profile to enable H2 database.
   */
  public static final String H2 = "nflow.db.h2";

  /**
   * Profile to enable MySQL database.
   */
  public static final String MYSQL = "nflow.db.mysql";

  /**
   * Profile to enable Oracle database.
   */
  public static final String ORACLE = "nflow.db.oracle";

  /**
   * Profile to enable PostgreSQL database.
   */
  public static final String POSTGRESQL = "nflow.db.postgresql";

  /**
   * Profile to enable SQL Server database.
   */
  public static final String SQLSERVER = "nflow.db.sqlserver";

  /**
   * Profile to enable JMX services.
   */
  public static final String JMX = "jmx";

  /**
   * Profile to enable Metrics.
   */
  public static final String METRICS = "metrics";

}
