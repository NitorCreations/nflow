package com.nitorcreations.nflow.engine.dao;

import static java.lang.System.currentTimeMillis;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.nitorcreations.nflow.engine.domain.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;

@Component
public class RepositoryDao {

  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;
  String nflowName;

  @Inject
  public RepositoryDao(DataSource dataSource, Environment env) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.namedJdbc = new NamedParameterJdbcTemplate(dataSource);
    this.nflowName = env.getProperty("nflow.instance.name");
    if (StringUtils.isEmpty(nflowName)) {
      this.nflowName = null;
    }
  }

  public int insertWorkflowInstance(WorkflowInstance instance) {
    try {
      return insertWorkflowInstanceImpl(instance);
    } catch (DuplicateKeyException ex) {
      return -1;
    }
  }

  private int insertWorkflowInstanceImpl(WorkflowInstance instance) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new WorkflowInstancePreparedStatementCreator(instance, true, nflowName), keyHolder);
    return keyHolder.getKey().intValue();
  }

  public void updateWorkflowInstance(WorkflowInstance instance) {
    jdbc.update(new WorkflowInstancePreparedStatementCreator(instance, false, nflowName));
  }

  public WorkflowInstance getWorkflowInstance(int id) {
    String sql = "select * from nflow_workflow where id = ?";
    return jdbc.queryForObject(sql, new WorkflowInstanceRowMapper(), id);
  }

  public List<Integer> pollNextWorkflowInstanceIds(int batchSize) {
    String ownerCondition = "and owner = '" + nflowName + "' ";
    if (StringUtils.isEmpty(nflowName)) {
      ownerCondition = "and owner is null ";
    }

    String sql =
      "select id from nflow_workflow " +
      "where is_processing is false " +
      "and next_activation < current_timestamp " + ownerCondition +
      "order by next_activation asc " +
      "limit " + batchSize;

    List<Integer> instanceIds = jdbc.query(sql, new RowMapper<Integer>() {
      @Override
      public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getInt("id");
      }
    });

    List<Object[]> batchArgs = new ArrayList<>();
    for (Integer instanceId : instanceIds) {
      batchArgs.add(new Object[] { instanceId });
    }
    int[] updateStatuses = jdbc.batchUpdate(
      "update nflow_workflow " +
      "set is_processing = true " +
      "where id = ? and is_processing = false",
      batchArgs);
    for (int status : updateStatuses) {
      if (status != 1) {
        throw new RuntimeException(
            "Race condition in polling workflow instances detected. " +
            "Multiple pollers using same name? (" + nflowName +")");
      }
    }

    return instanceIds;
  }

  public List<WorkflowInstance> queryWorkflowInstances(
          QueryWorkflowInstances query) {
    String sql = "select * from nflow_workflow";
    List<String> conditions = new ArrayList<>();
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (!isEmpty(query.types)) {
      conditions.add("type in (:types)");
      params.addValue("types", query.types);
    }
    if (!isEmpty(query.states)) {
      conditions.add("state in (:states)");
      params.addValue("states", query.states);
    }
    if (query.businessKey != null) {
      conditions.add("business_key = :business_key");
      params.addValue("business_key", query.businessKey);
    }
    if (!isEmpty(conditions)) {
      sql += " where ";
      boolean first = true;
      for (String cond : conditions) {
        if (first) {
          first = false;
        } else {
          sql += " and ";
        }
        sql += cond;
      }
    }
    return namedJdbc.query(sql, params, new WorkflowInstanceRowMapper());
  }

  public void insertWorkflowInstanceAction(WorkflowInstance action) {
    jdbc.update(
        "insert into nflow_workflow_action(workflow_id, state_next, state_next_text, next_activation)"
        + " values (?,?,?,?)", action.id, action.state, action.stateText, toTimestamp(action.nextActivation));
  }

  static class WorkflowInstancePreparedStatementCreator implements PreparedStatementCreator {

    private final WorkflowInstance instance;
    private final boolean isInsert;
    private final String owner;

    private final static String insertSql =
        "insert into nflow_workflow(type, business_key, owner, request_data, state, state_text, state_variables, "
        + "next_activation, is_processing) values (?,?,?,?,?,?,?,?,?)";

    private final static String updateSql =
        "update nflow_workflow "
        + "set state = ?, state_text = ?, state_variables = ?, next_activation = ?, "
        + "is_processing = ?, retries = ? where id = ?";

    public WorkflowInstancePreparedStatementCreator(WorkflowInstance instance, boolean isInsert, String owner) {
      this.isInsert = isInsert;
      this.instance = instance;
      this.owner = owner;
    }

    @Override
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
      PreparedStatement ps;
      int p = 1;
      if (isInsert) {
        ps = connection.prepareStatement(insertSql, new String[] {"id"});
        ps.setString(p++, instance.type);
        ps.setString(p++, instance.businessKey);;
        ps.setString(p++, owner);
        ps.setString(p++, instance.requestData);
      } else {
        ps = connection.prepareStatement(updateSql);
      }
      ps.setString(p++, instance.state);
      ps.setString(p++, instance.stateText);
      ps.setString(p++, new JSONMapper().mapToJson(instance.stateVariables));
      ps.setTimestamp(p++, toTimestamp(instance.nextActivation));
      ps.setBoolean(p++, instance.processing);
      if (!isInsert) {
        ps.setInt(p++, instance.retries);
        ps.setInt(p++, instance.id);
      }
      return ps;
    }
  }

  static Timestamp toTimestampOrNow(DateTime time) {
    return time == null ? new Timestamp(currentTimeMillis()) : new Timestamp(time.getMillis());
  }

  static Timestamp toTimestamp(DateTime time) {
    return time == null ? null : new Timestamp(time.getMillis());
  }

  static DateTime toDateTime(Timestamp time) {
    return time == null ? null : new DateTime(time.getTime());
  }

  static class WorkflowInstanceRowMapper implements RowMapper<WorkflowInstance> {
    @Override
    public WorkflowInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new WorkflowInstance.Builder()
        .setId(rs.getInt("id"))
        .setType(rs.getString("type"))
        .setBusinessKey(rs.getString("business_key"))
        .setState(rs.getString("state"))
        .setStateText(rs.getString("state_text"))
        .setStateVariables(new JSONMapper().jsonToMap(rs.getString("state_variables")))
        .setNextActivation(toDateTime(rs.getTimestamp("next_activation")))
        .setProcessing(rs.getBoolean("is_processing"))
        .setRequestData(rs.getString("request_data"))
        .setRetries(rs.getInt("retries"))
        .setCreated(toDateTime(rs.getTimestamp("created")))
        .setModified(toDateTime(rs.getTimestamp("modified")))
        .setOwner(rs.getString("owner"))
        .build();
    }

  }

  static Long getLong(ResultSet rs, String columnName) throws SQLException {
    long tmp = rs.getLong(columnName);
    return rs.wasNull() ? null : Long.valueOf(tmp);
  }

}
