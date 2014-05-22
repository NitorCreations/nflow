package com.nitorcreations.nflow.engine.dao;

import static java.lang.System.currentTimeMillis;
import static org.joda.time.DateTime.now;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.collectionToDelimitedString;
import static org.springframework.util.StringUtils.isEmpty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.domain.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.domain.WorkflowInstance;
import com.nitorcreations.nflow.engine.domain.WorkflowInstanceAction;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

@Component
public class RepositoryDao {

  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;
  String nflowName;

  @Inject
  public RepositoryDao(@Named("nflow-datasource") DataSource dataSource, Environment env) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.namedJdbc = new NamedParameterJdbcTemplate(dataSource);
    this.nflowName = env.getProperty("nflow.instance.name");
    if (isEmpty(nflowName)) {
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
    int id = keyHolder.getKey().intValue();
    insertVariables(id, 0, instance.stateVariables, Collections.<String, String>emptyMap());
    return id;
  }

  @SuppressWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  private void insertVariables(final int id, final int actionId, Map<String, String> stateVariables,
      final Map<String, String> originalStateVariables) {
    if (stateVariables == null) {
      return;
    }
    final Iterator<Entry<String, String>> variables = stateVariables.entrySet().iterator();
    jdbc.batchUpdate("insert into nflow_workflow_state (workflow_id, action_id, state_key, state_value) values (?,?,?,?)",
        new AbstractInterruptibleBatchPreparedStatementSetter() {
      @Override
      protected boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException {
        Entry<String, String> var;
        while (true) {
          if (!variables.hasNext()) {
            return false;
          }
          var = variables.next();
          String oldVal = originalStateVariables.get(var.getKey());
          if (oldVal == null || !oldVal.equals(var.getValue())) {
            break;
          }
        }
        ps.setInt(1, id);
        ps.setInt(2, actionId);
        ps.setString(3, var.getKey());
        ps.setString(4, var.getValue());
        return true;
      }
    });
  }

  public void updateWorkflowInstance(WorkflowInstance instance) {
    jdbc.update(new WorkflowInstancePreparedStatementCreator(instance, false, nflowName));
  }

  public WorkflowInstance getWorkflowInstance(int id) {
    String sql = "select * from nflow_workflow where id = ?";
    WorkflowInstance instance = jdbc.queryForObject(sql, new WorkflowInstanceRowMapper(), id);
    fillState(instance);
    return instance;
  }

  @SuppressWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  private void fillState(final WorkflowInstance instance) {
    jdbc.query(
      "select outside.state_key, outside.state_value from nflow_workflow_state outside inner join "
        + "(select workflow_id, max(action_id) action_id, state_key from nflow_workflow_state where workflow_id = ? group by workflow_id, state_key) inside "
        + "on outside.workflow_id = inside.workflow_id and outside.action_id = inside.action_id and outside.state_key = inside.state_key",
      new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        instance.stateVariables.put(rs.getString(1), rs.getString(2));
      }
    }, instance.id);
    instance.originalStateVariables.putAll(instance.stateVariables);
  }

  @SuppressWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  public List<Integer> pollNextWorkflowInstanceIds(int batchSize) {
    String ownerCondition = "and owner = '" + nflowName + "' ";
    if (isEmpty(nflowName)) {
      ownerCondition = "and owner is null ";
    }
    String sql =
      "select id from nflow_workflow where is_processing is false and next_activation < current_timestamp "
        + ownerCondition + "order by next_activation asc limit " + batchSize;
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
      "update nflow_workflow set is_processing = true where id = ? and is_processing = false",
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

  public List<WorkflowInstance> queryWorkflowInstances(QueryWorkflowInstances query) {
    String sql = "select * from nflow_workflow";

    List<String> conditions = new ArrayList<>();
    MapSqlParameterSource params = new MapSqlParameterSource();
    if (!isEmpty(query.ids)) {
      conditions.add("id in (:ids)");
      params.addValue("ids", query.ids);
    }
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
    if (query.externalId != null) {
      conditions.add("external_id = :external_id");
      params.addValue("external_id", query.externalId);
    }
    if (!isEmpty(conditions)) {
      sql += " where " + collectionToDelimitedString(conditions, " and ");
    }
    List<WorkflowInstance> ret = namedJdbc.query(sql, params, new WorkflowInstanceRowMapper());
    for (WorkflowInstance instance : ret) {
      fillState(instance);
    }
    if (query.includeActions) {
      for (WorkflowInstance instance : ret) {
        fillActions(instance);
      }
    }
    return ret;
  }

  private void fillActions(WorkflowInstance instance) {
    instance.actions.addAll(jdbc.query("select * from nflow_workflow_action where workflow_id = ? order by id asc",
        new WorkflowInstanceActionRowMapper(), instance.id));
  }

  @SuppressWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  public void insertWorkflowInstanceAction(final WorkflowInstance instance, final WorkflowInstanceAction action) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="findbugs does not trust jdbctemplate")
      public PreparedStatement createPreparedStatement(Connection con)
          throws SQLException {
        PreparedStatement p = con.prepareStatement(
            "insert into nflow_workflow_action(workflow_id, state, state_text, retry_no, execution_start, execution_end) values (?,?,?,?,?,?)",
            new String[] { "id" });
        p.setInt(1, action.workflowId);
        p.setString(2, action.state);
        p.setString(3, action.stateText);
        p.setInt(4, action.retryNo);
        p.setTimestamp(5, toTimestamp(action.executionStart));
        p.setTimestamp(6, toTimestamp(action.executionEnd));
        return p;
      }
    }, keyHolder);
    int actionId = keyHolder.getKey().intValue();
    insertVariables(action.workflowId, actionId, instance.stateVariables, instance.originalStateVariables);
  }

  static class WorkflowInstancePreparedStatementCreator implements PreparedStatementCreator {

    private final WorkflowInstance instance;
    private final boolean isInsert;
    private final String owner;

    private final static String insertSql =
        "insert into nflow_workflow(type, business_key, external_id, owner, request_data, state, state_text, "
        + "next_activation, is_processing) values (?,?,?,?,?,?,?,?,?)";

    private final static String updateSql =
        "update nflow_workflow set state = ?, state_text = ?, next_activation = ?, "
        + "is_processing = ?, retries = ?, modified = ? where id = ?";

    public WorkflowInstancePreparedStatementCreator(WorkflowInstance instance, boolean isInsert, String owner) {
      this.isInsert = isInsert;
      this.instance = instance;
      this.owner = owner;
    }

    @Override
    @SuppressWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="findbugs does not trust jdbctemplate")
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
      PreparedStatement ps;
      int p = 1;
      if (isInsert) {
        ps = connection.prepareStatement(insertSql, new String[] {"id"});
        ps.setString(p++, instance.type);
        ps.setString(p++, instance.businessKey);
        ps.setString(p++, instance.externalId);
        ps.setString(p++, owner);
        ps.setString(p++, instance.requestData);
      } else {
        ps = connection.prepareStatement(updateSql);
      }
      ps.setString(p++, instance.state);
      ps.setString(p++, instance.stateText);
      if (instance.nextActivation == null) {
        ps.setNull(p++, Types.TIMESTAMP);
      } else {
        ps.setTimestamp(p++, toTimestamp(instance.nextActivation));
      }
      ps.setBoolean(p++, instance.processing);
      if (!isInsert) {
        ps.setInt(p++, instance.retries);
        ps.setTimestamp(p++, toTimestamp(now()));
        ps.setInt(p++, instance.id);
      }
      return ps;
    }
  }

  static class WorkflowInstanceRowMapper implements RowMapper<WorkflowInstance> {
    @Override
    public WorkflowInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new WorkflowInstance.Builder()
        .setId(rs.getInt("id"))
        .setType(rs.getString("type"))
        .setBusinessKey(rs.getString("business_key"))
        .setExternalId(rs.getString("external_id"))
        .setState(rs.getString("state"))
        .setStateText(rs.getString("state_text"))
        .setStateVariables(new LinkedHashMap<String, String>())
        .setActions(new ArrayList<WorkflowInstanceAction>())
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

  static class WorkflowInstanceActionRowMapper implements RowMapper<WorkflowInstanceAction> {
    @Override
    public WorkflowInstanceAction mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new WorkflowInstanceAction.Builder()
        .setWorkflowId(rs.getInt("workflow_id"))
        .setState(rs.getString("state"))
        .setStateText(rs.getString("state_text"))
        .setRetryNo(rs.getInt("retry_no"))
        .setExecutionStart(toDateTime(rs.getTimestamp("execution_start")))
        .setExecutionEnd(toDateTime(rs.getTimestamp("execution_end")))
        .build();
    }
  }

  static Long getLong(ResultSet rs, String columnName) throws SQLException {
    long tmp = rs.getLong(columnName);
    return rs.wasNull() ? null : Long.valueOf(tmp);
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

}
