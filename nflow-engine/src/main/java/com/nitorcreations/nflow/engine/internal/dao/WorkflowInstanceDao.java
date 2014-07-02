package com.nitorcreations.nflow.engine.internal.dao;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.collectionToDelimitedString;
import static org.springframework.util.StringUtils.isEmpty;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.apache.commons.lang3.mutable.MutableInt;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.context.annotation.DependsOn;
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

import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@DependsOn("nflowDatabaseInitializer")
public class WorkflowInstanceDao {

  private static final Logger logger = getLogger(WorkflowInstanceDao.class);

  private final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;
  private int executorId;
  final String executorGroup;

  @Inject
  public WorkflowInstanceDao(@Named("nflow-datasource") DataSource dataSource, Environment env) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.namedJdbc = new NamedParameterJdbcTemplate(dataSource);
    this.executorGroup = trimToNull(env.getProperty("nflow.executor.group"));
    logger.info("Using nflow executor group " + executorGroup);
  }

  @PostConstruct
  private void allocateExecutorId() {
    final String host;
    final int pid;
    try {
      host = InetAddress.getLocalHost().getCanonicalHostName();
      pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to obatain host name and pid of running jvm", ex);
    }
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="findbugs does not trust jdbctemplate")
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement p = con.prepareStatement(
            "insert into nflow_executor(host, pid, executor_group) values (?,?,?)",
            new String[] { "id" });
        p.setString(1, host);
        p.setInt(2, pid);
        p.setString(3, executorGroup);
        return p;
      }
    }, keyHolder);
    executorId = keyHolder.getKey().intValue();
    logger.info("Using nflow executor id " + executorId);
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
    jdbc.update(new WorkflowInstancePreparedStatementCreator(instance, true, executorGroup, executorId), keyHolder);
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
    jdbc.update(new WorkflowInstancePreparedStatementCreator(instance, false, executorGroup, executorId));
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
  public List<Integer> pollNextWorkflowInstanceIds(int batchSize) {
    String groupCondition = "and executor_group = '" + executorGroup + "'";
    if (isEmpty(executorGroup)) {
      groupCondition = "and executor_group is null";
    }
    String sql =
      "select id from nflow_workflow where executor_id is null and next_activation < current_timestamp "
        + groupCondition + " order by next_activation asc limit " + batchSize;
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
      "update nflow_workflow set executor_id = " + executorId + " where id = ? and executor_id is null",
      batchArgs);
    for (int status : updateStatuses) {
      if (status != 1) {
        throw new RuntimeException(
            "Race condition in polling workflow instances detected. " +
            "Multiple pollers using same name? (" + executorGroup +")");
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

  @SuppressFBWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  public void insertWorkflowInstanceAction(final WorkflowInstance instance, final WorkflowInstanceAction action) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="findbugs does not trust jdbctemplate")
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
    private final String executorGroup;
    private final int executorId;

    private final static String insertSql =
        "insert into nflow_workflow(type, business_key, external_id, executor_group, state, state_text, "
        + "next_activation) values (?,?,?,?,?,?,?)";

    private final static String updateSql =
        "update nflow_workflow set state = ?, state_text = ?, next_activation = ?, "
        + "executor_id = ?, retries = ? where id = ?";

    public WorkflowInstancePreparedStatementCreator(WorkflowInstance instance, boolean isInsert, String executorGroup, int executorId) {
      this.isInsert = isInsert;
      this.instance = instance;
      this.executorGroup = executorGroup;
      this.executorId = executorId;
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
      ps.setString(p++, instance.stateText);
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
      return new WorkflowInstance.Builder()
        .setId(rs.getInt("id"))
        .setType(rs.getString("type"))
        .setBusinessKey(rs.getString("business_key"))
        .setExternalId(rs.getString("external_id"))
        .setState(rs.getString("state"))
        .setStateText(rs.getString("state_text"))
        .setActions(new ArrayList<WorkflowInstanceAction>())
        .setNextActivation(toDateTime(rs.getTimestamp("next_activation")))
        .setProcessing(rs.getObject("executor_id") != null)
        .setRetries(rs.getInt("retries"))
        .setCreated(toDateTime(rs.getTimestamp("created")))
        .setModified(toDateTime(rs.getTimestamp("modified")))
        .setOwner(rs.getString("executor_group"))
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
