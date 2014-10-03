package com.nitorcreations.nflow.engine.internal.dao;

import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.collectionToDelimitedString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.apache.commons.lang3.mutable.MutableInt;
import org.joda.time.DateTime;
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
import org.springframework.transaction.annotation.Transactional;

import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
public class WorkflowInstanceDao {

  // TODO: fetch text field max sizes from database meta data
  private static final int STATE_TEXT_LENGTH = 128;

  private JdbcTemplate jdbc;
  private NamedParameterJdbcTemplate namedJdbc;
  ExecutorDao executorInfo;

  /**
   * Use setter injection because having the dataSource in constructor may not work
   * when nFlow is used in some legacy systems.
   * @param dataSource The nFlow data source.
   */
  @Inject
  public void setDataSource(@Named("nflowDatasource") DataSource dataSource) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.namedJdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  @Inject
  public void setExecutorDao(ExecutorDao executorDao) {
    this.executorInfo = executorDao;
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
    jdbc.update(new WorkflowInstancePreparedStatementCreator(instance, true, executorInfo), keyHolder);
    int id = keyHolder.getKey().intValue();
    insertVariables(id, 0, instance.stateVariables, Collections.<String, String>emptyMap());
    return id;
  }

  @SuppressFBWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  private void insertVariables(final int id, final int actionId, Map<String, String> stateVariables,
      final Map<String, String> originalStateVariables) {
    if (stateVariables == null) {
      return;
    }
    final Iterator<Entry<String, String>> variables = stateVariables.entrySet().iterator();
    final MutableInt expectedCount = new MutableInt(0);
    int[] updateStatus = jdbc.batchUpdate("insert into nflow_workflow_state (workflow_id, action_id, state_key, state_value) values (?,?,?,?)",
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
        expectedCount.add(1);
        return true;
      }
    });
    int updatedRows = 0;
    for (int i=0; i<updateStatus.length; ++i) {
      updatedRows += updateStatus[i];
    }
    if (updatedRows != expectedCount.intValue()) {
      throw new IllegalStateException("Failed to insert/update state variables, expected update count " + expectedCount.intValue() + ", actual " + updatedRows);
    }
  }

  public void updateWorkflowInstance(WorkflowInstance instance) {
    jdbc.update(new WorkflowInstancePreparedStatementCreator(instance, false, executorInfo));
  }

  public boolean wakeupWorkflowInstanceIfNotExecuting(long id, String[] expectedStates) {
    StringBuilder sql = new StringBuilder("update nflow_workflow set next_activation = current_timestamp where id = ? and executor_id is null and (next_activation is null or next_activation > current_timestamp)");
    Object[] args = new Object[1 + expectedStates.length];
    args[0] = id;
    if (expectedStates.length > 0) {
      sql.append(" and state in (");
      for (int i = 0; i < expectedStates.length; i++) {
        sql.append("?,");
        args[i+1] = expectedStates[i];
      }
      sql.setCharAt(sql.length() - 1, ')');
    }
    return jdbc.update(sql.toString(), args) == 1;
  }

  public WorkflowInstance getWorkflowInstance(int id) {
    String sql = "select * from nflow_workflow where id = ?";
    WorkflowInstance instance = jdbc.queryForObject(sql, new WorkflowInstanceRowMapper(), id);
    fillState(instance);
    return instance;
  }

  @SuppressFBWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
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

  @SuppressFBWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  @Transactional
  public List<Integer> pollNextWorkflowInstanceIds(int batchSize) {
    String sql =
      "select id from nflow_workflow where executor_id is null and next_activation < current_timestamp and "
        + executorInfo.getExecutorGroupCondition() + " order by next_activation asc limit " + batchSize;
    List<Integer> instanceIds = jdbc.query(sql, new RowMapper<Integer>() {
      @Override
      public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getInt("id");
      }
    });
    Collections.sort(instanceIds);
    List<Object[]> batchArgs = new ArrayList<>();
    for (Integer instanceId : instanceIds) {
      batchArgs.add(new Object[] { instanceId });
    }
    int[] updateStatuses = jdbc.batchUpdate(
      "update nflow_workflow set executor_id = " + executorInfo.getExecutorId() + " where id = ? and executor_id is null",
      batchArgs);
    for (int status : updateStatuses) {
      if (status != 1) {
        throw new PollingRaceConditionException(
            "Race condition in polling workflow instances detected. " +
            "Multiple pollers using same name (" + executorInfo.getExecutorGroup() +")");
      }
    }
    return instanceIds;
  }

  public List<WorkflowInstance> queryWorkflowInstances(QueryWorkflowInstances query) {
    String sql = "select * from nflow_workflow";

    List<String> conditions = new ArrayList<>();
    MapSqlParameterSource params = new MapSqlParameterSource();
    conditions.add(executorInfo.getExecutorGroupCondition());
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

  public void insertWorkflowInstanceAction(final WorkflowInstance instance, final WorkflowInstanceAction action) {
    int actionId = insertWorkflowInstanceAction(action);
    insertVariables(action.workflowInstanceId, actionId, instance.stateVariables, instance.originalStateVariables);
  }

  @SuppressFBWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  public int insertWorkflowInstanceAction(final WorkflowInstanceAction action) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="findbugs does not trust jdbctemplate")
      public PreparedStatement createPreparedStatement(Connection con)
          throws SQLException {
        PreparedStatement p = con.prepareStatement(
            "insert into nflow_workflow_action(workflow_id, executor_id, state, state_text, retry_no, execution_start, execution_end) values (?,?,?,?,?,?,?)",
            new String[] { "id" });
        p.setInt(1, action.workflowInstanceId);
        p.setInt(2, executorInfo.getExecutorId());
        p.setString(3, action.state);
        p.setString(4, limitLength(action.stateText, STATE_TEXT_LENGTH));
        p.setInt(5, action.retryNo);
        p.setTimestamp(6, toTimestamp(action.executionStart));
        p.setTimestamp(7, toTimestamp(action.executionEnd));
        return p;
      }
    }, keyHolder);
    return keyHolder.getKey().intValue();
  }

  static class WorkflowInstancePreparedStatementCreator implements PreparedStatementCreator {

    private final WorkflowInstance instance;
    private final boolean isInsert;
    private final String executorGroup;
    private final int executorId;

    private final static String insertSql =
        "insert into nflow_workflow(type, business_key, external_id, executor_group, state, state_text, "
        + "next_activation) values (?,?,?,?,?,?,?)";

    private final static String updateSql =
        "update nflow_workflow set state = ?, state_text = ?, next_activation = ?, "
        + "executor_id = ?, retries = ? where id = ?";

    public WorkflowInstancePreparedStatementCreator(WorkflowInstance instance, boolean isInsert, ExecutorDao executorInfo) {
      this.isInsert = isInsert;
      this.instance = instance;
      this.executorGroup = executorInfo.getExecutorGroup();
      this.executorId = executorInfo.getExecutorId();
    }

    @Override
    @SuppressWarnings("resource")
    @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="findbugs does not trust jdbctemplate")
    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
      PreparedStatement ps;
      int p = 1;
      if (isInsert) {
        ps = connection.prepareStatement(insertSql, new String[] {"id"});
        ps.setString(p++, instance.type);
        ps.setString(p++, instance.businessKey);
        ps.setString(p++, instance.externalId);
        ps.setString(p++, executorGroup);
      } else {
        ps = connection.prepareStatement(updateSql);
      }
      ps.setString(p++, instance.state);
      ps.setString(p++, limitLength(instance.stateText, STATE_TEXT_LENGTH));
      ps.setTimestamp(p++, toTimestamp(instance.nextActivation));
      if (!isInsert) {
        ps.setObject(p++, instance.processing ? executorId : null);
        ps.setInt(p++, instance.retries);
        ps.setInt(p++, instance.id);
      }
      return ps;
    }
  }

  static class WorkflowInstanceRowMapper implements RowMapper<WorkflowInstance> {
    @Override
    public WorkflowInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
      Integer executorId = (Integer) rs.getObject("executor_id");
      return new WorkflowInstance.Builder()
        .setId(rs.getInt("id"))
        .setExecutorId(executorId)
        .setType(rs.getString("type"))
        .setBusinessKey(rs.getString("business_key"))
        .setExternalId(rs.getString("external_id"))
        .setState(rs.getString("state"))
        .setStateText(rs.getString("state_text"))
        .setActions(new ArrayList<WorkflowInstanceAction>())
        .setNextActivation(toDateTime(rs.getTimestamp("next_activation")))
        .setProcessing(executorId != null)
        .setRetries(rs.getInt("retries"))
        .setCreated(toDateTime(rs.getTimestamp("created")))
        .setModified(toDateTime(rs.getTimestamp("modified")))
        .setExecutorGroup(rs.getString("executor_group"))
        .build();
    }

  }

  static class WorkflowInstanceActionRowMapper implements RowMapper<WorkflowInstanceAction> {
    @Override
    public WorkflowInstanceAction mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new WorkflowInstanceAction.Builder()
        .setWorkflowInstanceId(rs.getInt("workflow_id"))
        .setExecutorId(rs.getInt("executor_id"))
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

  static String limitLength(String s, int maxLen) {
    if (s == null) {
      return null;
    }
    if (s.length() < maxLen) {
      return s;
    }
    return s.substring(0, maxLen);
  }
}
