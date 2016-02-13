package com.nitorcreations.nflow.engine.internal.dao;

import java.sql.ResultSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.nitorcreations.nflow.engine.internal.config.NFlow;

@Named
public class HealthCheckDao {
  private JdbcTemplate jdbc;

  public void checkDatabaseConnection() {
    jdbc.query("select status, type from nflow_workflow where id = 0", new ResultSetExtractor<Object>() {
      @Override
      public Object extractData(ResultSet resultSet) throws DataAccessException {
        return null;
      }
    });
  }

  @Inject
  public void setJdbcTemplate(@NFlow JdbcTemplate jdbcTemplate) {
    this.jdbc = jdbcTemplate;
  }
}
