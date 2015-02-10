package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.toTimestamp;
import static com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao.FirstColumnLengthExtractor.firstColumnLengthExtractor;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.paused;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.stopped;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.sort;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.join;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.collectionToDelimitedString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.engine.internal.storage.db.SQLVariants;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecutionStatistics;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Use setter injection because constructor injection may not work when nFlow is
 * used in some legacy systems.
 */
@Component
public class WorkflowInstanceDao {

  static final Map<String, String> EMPTY_STATE_MAP = Collections.<String,String>emptyMap();
  static final Map<Integer, Map<String, String>> EMPTY_ACTION_STATE_MAP = Collections.<Integer, Map<String, String>>emptyMap();

  private static final String GET_STATISTICS_PREFIX = "select state, count(1) as amount from nflow_workflow where executor_group = ? and type = ?";

  JdbcTemplate jdbc;
  private NamedParameterJdbcTemplate namedJdbc;
  private TransactionTemplate transaction;
  ExecutorDao executorInfo;
  SQLVariants sqlVariants;
  private long workflowInstanceQueryMaxResults;
  private long workflowInstanceQueryMaxResultsDefault;
  int instanceStateTextLength;
  int actionStateTextLength;

  @Inject
  public void setSQLVariants(SQLVariants sqlVariants) {
    this.sqlVariants = sqlVariants;
  }

  @Inject
  public void setJdbcTemplate(@NFlow JdbcTemplate nflowJdbcTemplate) {
    this.jdbc = nflowJdbcTemplate;
  }

  @Inject
  public void setTransactionTemplate(@NFlow TransactionTemplate transactionTemplate) {
    this.transaction = transactionTemplate;
  }

  @Inject
  public void setNamedParameterJdbcTemplate(@NFlow NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate) {
    this.namedJdbc = nflowNamedParameterJdbcTemplate;
  }

  @Inject
  public void setExecutorDao(ExecutorDao executorDao) {
    this.executorInfo = executorDao;
  }

  @Inject
  public void setEnvironment(Environment env) {
    workflowInstanceQueryMaxResults = env.getRequiredProperty("nflow.workflow.instance.query.max.results", Long.class);
    workflowInstanceQueryMaxResultsDefault = env.getRequiredProperty("nflow.workflow.instance.query.max.results.default",
        Long.class);
  }

  @PostConstruct
  public void findColumnMaxLengths() {
    instanceStateTextLength = jdbc.query("select state_text from nflow_workflow where 1 = 0", firstColumnLengthExtractor);
    actionStateTextLength = jdbc.query("select state_text from nflow_workflow_action where 1 = 0", firstColumnLengthExtractor);
  }

  public int insertWorkflowInstance(WorkflowInstance instance) {
    try {
      if (sqlVariants.hasUpdateableCTE()) {
        return insertWorkflowInstanceWithCte(instance);
      }
      return insertWorkflowInstanceWithTransaction(instance);
    } catch (DuplicateKeyException ex) {
      return -1;
    }
  }

  private int insertWorkflowInstanceWithCte(WorkflowInstance instance) {
    StringBuilder sqlb = new StringBuilder(256);
    sqlb.append("with wf as (" + insertWorkflowInstanceSql() + " returning id)");
    int pos = 8;
    Object[] args = Arrays.copyOf(
        new Object[] { instance.type, instance.businessKey, instance.externalId, executorInfo.getExecutorGroup(),
            instance.status.name(), instance.state, abbreviate(instance.stateText, instanceStateTextLength),
            toTimestamp(instance.nextActivation) },
        pos + instance.stateVariables.size() * 2);
    for (Entry<String, String> var : instance.stateVariables.entrySet()) {
      sqlb.append(", ins").append(pos).append(" as (").append(insertWorkflowInstanceStateSql())
          .append(" select wf.id,0,?,? from wf)");
      args[pos++] = var.getKey();
      args[pos++] = var.getValue();
    }
    sqlb.append(" select wf.id from wf");
    return jdbc.queryForObject(sqlb.toString(), Integer.class, args);
  }

  String insertWorkflowInstanceSql() {
    return "insert into nflow_workflow(type, business_key, external_id, executor_group, status, state, state_text, "
        + "next_activation) values (?, ?, ?, ?, " + sqlVariants.castToEnumType("?", "workflow_status") + ", ?, ?, ?)";
  }

  String insertWorkflowInstanceStateSql() {
    return "insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value)";
  }

  private int insertWorkflowInstanceWithTransaction(final WorkflowInstance instance) {
    return transaction.execute(new TransactionCallback<Integer>() {
      @Override
      public Integer doInTransaction(TransactionStatus status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(new PreparedStatementCreator() {
          @Override
          @SuppressFBWarnings(value = "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification = "findbugs does not trust jdbctemplate")
          public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            PreparedStatement ps;
            int p = 1;
            ps = connection.prepareStatement(insertWorkflowInstanceSql(), new String[] { "id" });
            ps.setString(p++, instance.type);
            ps.setString(p++, instance.businessKey);
            ps.setString(p++, instance.externalId);
            ps.setString(p++, executorInfo.getExecutorGroup());
            ps.setString(p++, instance.status.name());
            ps.setString(p++, instance.state);
            ps.setString(p++, abbreviate(instance.stateText, instanceStateTextLength));
            ps.setTimestamp(p++, toTimestamp(instance.nextActivation));
            return ps;
          }
        }, keyHolder);
        int id = keyHolder.getKey().intValue();
        insertVariables(id, 0, instance.stateVariables, EMPTY_STATE_MAP);
        return id;
      }
    });
  }

  private Map<String, String> changedStateVariables(Map<String, String> stateVariables, Map<String, String> originalStateVariables) {
    if (stateVariables == null) {
      return emptyMap();
    }
    Map<String, String> changedVariables = new HashMap<>(stateVariables.size());
    for (Entry<String, String> current : stateVariables.entrySet()) {
      String oldVal = originalStateVariables.get(current.getKey());
      if (oldVal == null || !oldVal.equals(current.getValue())) {
        changedVariables.put(current.getKey(), current.getValue());
      }
    }
    return changedVariables;
  }

  @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "common jdbctemplate practice")
  void insertVariables(final int id, final int actionId, Map<String, String> stateVariables, Map<String, String> originalStateVariables) {
    Map<String, String> changedStateVariables = changedStateVariables(stateVariables, originalStateVariables);
    if (changedStateVariables.isEmpty()) {
      return;
    }
    final Iterator<Entry<String, String>> variables = changedStateVariables.entrySet().iterator();
    int[] updateStatus = jdbc.batchUpdate(insertWorkflowInstanceStateSql() + " values (?,?,?,?)",
        new AbstractInterruptibleBatchPreparedStatementSetter() {
      @Override
      protected boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException {
        if (!variables.hasNext()) {
          return false;
        }
        Entry<String, String> var = variables.next();
        ps.setInt(1, id);
        ps.setInt(2, actionId);
        ps.setString(3, var.getKey());
        ps.setString(4, var.getValue());
        return true;
      }
    });
    int updatedRows = 0;
    for (int i=0; i<updateStatus.length; ++i) {
      updatedRows += updateStatus[i];
    }
    if (updatedRows != changedStateVariables.size()) {
      throw new IllegalStateException("Failed to insert/update state variables, expected update count " + changedStateVariables.size() + ", actual " + updatedRows);
    }
  }

  public void updateWorkflowInstanceAfterExecution(WorkflowInstance instance, WorkflowInstanceAction action) {
    if (sqlVariants.hasUpdateableCTE()) {
      updateWorkflowInstanceWithCte(instance, action);
    } else {
      updateWorkflowInstanceWithTransaction(instance, action);
    }
  }

  public void updateWorkflowInstance(WorkflowInstance instance) {
    jdbc.update(updateWorkflowInstanceSql(), instance.status.name(), instance.state,
        abbreviate(instance.stateText, instanceStateTextLength), toTimestamp(instance.nextActivation),
        instance.status == executing ? executorInfo.getExecutorId() : null, instance.retries,
        instance.id);
  }

  private void updateWorkflowInstanceWithTransaction(final WorkflowInstance instance, final WorkflowInstanceAction action) {
    transaction.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        updateWorkflowInstance(instance);
        insertWorkflowInstanceAction(instance, action);
      }
    });
  }

  private void updateWorkflowInstanceWithCte(WorkflowInstance instance, final WorkflowInstanceAction action) {
    int executorId = executorInfo.getExecutorId();
    StringBuilder sqlb = new StringBuilder(256);
    sqlb.append("with wf as (").append(updateWorkflowInstanceSql()).append(" returning id), ");
    sqlb.append("act as (").append(insertWorkflowActionSql()).append(" select wf.id,?,")
        .append(sqlVariants.castToEnumType("?", "action_type")).append(",?,?,?,?,? from wf returning id)");
    Map<String, String> changedStateVariables = changedStateVariables(instance.stateVariables, instance.originalStateVariables);
    int pos = 14;
    Object[] args = Arrays.copyOf(
        new Object[] { instance.status.name(), instance.state, abbreviate(instance.stateText, instanceStateTextLength),
            toTimestamp(instance.nextActivation), instance.status == executing ? executorId : null, instance.retries, instance.id,
            executorId, action.type.name(), action.state, abbreviate(action.stateText, actionStateTextLength), action.retryNo,
            toTimestamp(action.executionStart), toTimestamp(action.executionEnd) }, pos + changedStateVariables.size() * 2);
    for (Entry<String, String> var : changedStateVariables.entrySet()) {
      sqlb.append(", ins").append(pos).append(" as (").append(insertWorkflowInstanceStateSql())
          .append(" select wf.id,act.id,?,? from wf,act)");
      args[pos++] = var.getKey();
      args[pos++] = var.getValue();
    }
    sqlb.append(" select act.id from act");
    jdbc.queryForObject(sqlb.toString(), Integer.class, args);
  }

  String insertWorkflowActionSql() {
    return "insert into nflow_workflow_action(workflow_id, executor_id, type, state, state_text, retry_no, execution_start, execution_end)";
  }

  private String updateWorkflowInstanceSql() {
    return "update nflow_workflow set status = " + sqlVariants.castToEnumType("?", "workflow_status")
        + ", state = ?, state_text = ?, next_activation = ?, executor_id = ?, retries = ? where id = ? and executor_id = "
        + executorInfo.getExecutorId();
  }

  public boolean updateNotRunningWorkflowInstance(long id, String state, DateTime nextActivation, WorkflowInstanceStatus status) {
    List<String> vars = new ArrayList<>();
    List<Object> args = new ArrayList<>();
    if (state != null) {
      vars.add("state = ?, retries = 0");
      args.add(state);
    }
    if (nextActivation != null) {
      vars.add("next_activation = ?");
      args.add(toTimestamp(nextActivation));
    }
    if (status != null) {
      vars.add("status = " + sqlVariants.castToEnumType("?", "workflow_status"));
      args.add(status.name());
    }
    String sql = "update nflow_workflow set " + join(vars, ", ") + " where id = ? and executor_id is null";
    args.add(id);
    return jdbc.update(sql, args.toArray()) == 1;
  }

  public boolean stopNotRunningWorkflowInstance(long id, String stateText) {
    return jdbc.update("update nflow_workflow set next_activation = null, status = '" + stopped
        + "', state_text = ? where id = ? and executor_id is null and next_activation is not null", stateText, id) == 1;
  }

  public boolean pauseNotRunningWorkflowInstance(long id, String stateText) {
    return jdbc.update("update nflow_workflow set status = '" + paused
        + "', state_text = ? where id = ? and executor_id is null and next_activation is not null", stateText, id) == 1;
  }

  public boolean resumePausedWorkflowInstance(long id, String stateText) {
    return jdbc.update("update nflow_workflow set status = '" + inProgress + "', state_text = ? where id = ? and status = '"
        + paused + "'", stateText, id) == 1;
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
    String sql = "select w.*, "
        + "(select min(execution_start) from nflow_workflow_action a where a.workflow_id = w.id) as started "
        + "from nflow_workflow w where w.id = ?";
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

  @SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "common jdbctemplate practice")
  public List<Integer> pollNextWorkflowInstanceIds(final int batchSize) {
    if (sqlVariants.hasUpdateReturning()) {
      return pollNextWorkflowInstanceIdsWithUpdateReturning(batchSize);
    }
    return pollNextWorkflowInstanceIdsWithTransaction(batchSize);
  }

  String updateInstanceForExecutionQuery() {
    return "update nflow_workflow set executor_id = " + executorInfo.getExecutorId() + ", status = '" + executing.name() + "'";
  }

  String whereConditionForInstanceUpdate(int batchSize) {
    return "where executor_id is null and status in ('" + created.name() + "', '" + inProgress.name()
        + "') and next_activation < current_timestamp and " + executorInfo.getExecutorGroupCondition()
        + " order by next_activation asc limit " + batchSize;
  }

  private List<Integer> pollNextWorkflowInstanceIdsWithUpdateReturning(int batchSize) {
    return jdbc.queryForList(updateInstanceForExecutionQuery() + " where id in (select id from nflow_workflow "
        + whereConditionForInstanceUpdate(batchSize) + ") and executor_id is null returning id", Integer.class);
  }

  private List<Integer> pollNextWorkflowInstanceIdsWithTransaction(final int batchSize) {
    return transaction.execute(new TransactionCallback<List<Integer>>() {
      @Override
      public List<Integer> doInTransaction(TransactionStatus transactionStatus) {
        String sql = "select id, modified from nflow_workflow " + whereConditionForInstanceUpdate(batchSize);
        List<OptimisticLockKey> instances = jdbc.query(sql, new RowMapper<OptimisticLockKey>() {
          @Override
          public OptimisticLockKey mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new OptimisticLockKey(rs.getInt("id"), rs.getTimestamp("modified"));
          }
        });
        sort(instances);
        List<Object[]> batchArgs = new ArrayList<>(instances.size());
        List<Integer> ids = new ArrayList<>(instances.size());
        for (OptimisticLockKey instance : instances) {
          batchArgs.add(new Object[] { instance.id, instance.modified });
          ids.add(instance.id);
        }
        int[] updateStatuses = jdbc.batchUpdate(updateInstanceForExecutionQuery()
            + " where id = ? and modified = ? and executor_id is null", batchArgs);
        Iterator<Integer> idIt = ids.iterator();
        for (int status : updateStatuses) {
          idIt.next();
          if (status == 0) {
            idIt.remove();
            if (ids.isEmpty()) {
              throw new PollingRaceConditionException("Race condition in polling workflow instances detected. "
                  + "Multiple pollers using same name (" + executorInfo.getExecutorGroup() + ")");
            }
            continue;
          }
          if (status != 1) {
            throw new PollingRaceConditionException("Race condition in polling workflow instances detected. "
                + "Multiple pollers using same name (" + executorInfo.getExecutorGroup() + ")");
          }
        }
        return ids;
      }
    });
  }

  static final class FirstColumnLengthExtractor implements org.springframework.jdbc.core.ResultSetExtractor<Integer> {
    static final FirstColumnLengthExtractor firstColumnLengthExtractor = new FirstColumnLengthExtractor();

    @Override
    public Integer extractData(ResultSet rs) throws SQLException, DataAccessException {
      return rs.getMetaData().getColumnDisplaySize(1);
    }
  }

  private static class OptimisticLockKey implements Comparable<OptimisticLockKey> {
    public final int id;
    public final Timestamp modified;

    public OptimisticLockKey(int id, Timestamp modified) {
      this.id = id;
      this.modified = modified;
    }

    @Override
    public int compareTo(OptimisticLockKey other) {
      return this.id - other.id;
    }
  }

  public List<WorkflowInstance> queryWorkflowInstances(QueryWorkflowInstances query) {
    String sql = "select w.*, "
        + "(select min(execution_start) from nflow_workflow_action a where a.workflow_id = w.id) as started "
        + "from nflow_workflow w ";

    List<String> conditions = new ArrayList<>();
    MapSqlParameterSource params = new MapSqlParameterSource();
    conditions.add(executorInfo.getExecutorGroupCondition());
    if (!isEmpty(query.ids)) {
      conditions.add("w.id in (:ids)");
      params.addValue("ids", query.ids);
    }
    if (!isEmpty(query.types)) {
      conditions.add("w.type in (:types)");
      params.addValue("types", query.types);
    }
    if (!isEmpty(query.states)) {
      conditions.add("w.state in (:states)");
      params.addValue("states", query.states);
    }
    if (query.businessKey != null) {
      conditions.add("w.business_key = :business_key");
      params.addValue("business_key", query.businessKey);
    }
    if (query.externalId != null) {
      conditions.add("w.external_id = :external_id");
      params.addValue("external_id", query.externalId);
    }
    if (!isEmpty(conditions)) {
      sql += " where " + collectionToDelimitedString(conditions, " and ");
    }
    sql += " limit :limit";
    params.addValue("limit", getMaxResults(query.maxResults));
    List<WorkflowInstance> ret = namedJdbc.query(sql, params, new WorkflowInstanceRowMapper());
    for (WorkflowInstance instance : ret) {
      fillState(instance);
    }
    if (query.includeActions) {
      for (WorkflowInstance instance : ret) {
        fillActions(instance, query.includeActionStateVariables);
      }
    }
    return ret;
  }

  private long getMaxResults(Long maxResults) {
    if (maxResults == null) {
      return workflowInstanceQueryMaxResultsDefault;
    }
    if (maxResults.longValue() > workflowInstanceQueryMaxResults) {
      return workflowInstanceQueryMaxResults;
    }
    return maxResults.longValue();
  }

  private void fillActions(WorkflowInstance instance, boolean includeStateVariables) {
    Map<Integer, Map<String, String>> actionStates = includeStateVariables ? fetchActionStateVariables(instance) :
      EMPTY_ACTION_STATE_MAP;
    instance.actions.addAll(jdbc.query("select * from nflow_workflow_action where workflow_id = ? order by id asc",
        new WorkflowInstanceActionRowMapper(actionStates), instance.id));
  }

  private Map<Integer, Map<String, String>> fetchActionStateVariables(WorkflowInstance instance) {
    return jdbc.query("select * from nflow_workflow_state where workflow_id = ? order by action_id, state_key asc",
        new WorkflowActionStateRowMapper(), instance.id);
  }

  @Transactional(propagation = MANDATORY)
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
            insertWorkflowActionSql() + " values (?,?," + sqlVariants.castToEnumType("?", "action_type") + ",?,?,?,?,?)",
            new String[] { "id" });
        int field = 1;
        p.setInt(field++, action.workflowInstanceId);
        p.setInt(field++, executorInfo.getExecutorId());
        p.setString(field++, action.type.name());
        p.setString(field++, action.state);
        p.setString(field++, abbreviate(action.stateText, actionStateTextLength));
        p.setInt(field++, action.retryNo);
        p.setTimestamp(field++, toTimestamp(action.executionStart));
        p.setTimestamp(field++, toTimestamp(action.executionEnd));
        return p;
      }
    }, keyHolder);
    return keyHolder.getKey().intValue();
  }

  public String getWorkflowInstanceState(int workflowInstanceId) {
    return jdbc.queryForObject("select state from nflow_workflow where id = ?", String.class, workflowInstanceId);
  }

  static class WorkflowInstanceRowMapper implements RowMapper<WorkflowInstance> {
    @Override
    public WorkflowInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
      Integer executorId = (Integer) rs.getObject("executor_id");
      return new WorkflowInstance.Builder()
        .setId(rs.getInt("id"))
        .setExecutorId(executorId)
          .setStatus(WorkflowInstanceStatus.valueOf(rs.getString("status")))
        .setType(rs.getString("type"))
        .setBusinessKey(rs.getString("business_key"))
        .setExternalId(rs.getString("external_id"))
        .setState(rs.getString("state"))
        .setStateText(rs.getString("state_text"))
        .setActions(new ArrayList<WorkflowInstanceAction>())
        .setNextActivation(toDateTime(rs.getTimestamp("next_activation")))
        .setRetries(rs.getInt("retries"))
        .setCreated(toDateTime(rs.getTimestamp("created")))
        .setModified(toDateTime(rs.getTimestamp("modified")))
        .setStarted(toDateTime(rs.getTimestamp("started")))
        .setExecutorGroup(rs.getString("executor_group"))
        .build();
    }
  }

  static class WorkflowInstanceActionRowMapper implements RowMapper<WorkflowInstanceAction> {
    private final Map<Integer, Map<String, String>> actionStates;
    public WorkflowInstanceActionRowMapper(Map<Integer, Map<String, String>> actionStates) {
      this.actionStates = actionStates;
    }
    @Override
    public WorkflowInstanceAction mapRow(ResultSet rs, int rowNum) throws SQLException {
      int actionId = rs.getInt("id");
      Map<String, String> actionState = actionStates.containsKey(actionId) ? actionStates.get(actionId) :
        EMPTY_STATE_MAP;
      return new WorkflowInstanceAction.Builder()
        .setWorkflowInstanceId(rs.getInt("workflow_id"))
        .setExecutorId(rs.getInt("executor_id"))
        .setType(WorkflowActionType.valueOf(rs.getString("type")))
        .setState(rs.getString("state"))
        .setStateText(rs.getString("state_text"))
        .setUpdatedStateVariables(actionState)
        .setRetryNo(rs.getInt("retry_no"))
        .setExecutionStart(toDateTime(rs.getTimestamp("execution_start")))
        .setExecutionEnd(toDateTime(rs.getTimestamp("execution_end")))
        .build();
    }
  }

  static class WorkflowActionStateRowMapper implements ResultSetExtractor<Map<Integer, Map<String, String>>> {
    private final Map<Integer, Map<String, String>> actionStates = new LinkedHashMap<>();

    @Override
    public Map<Integer, Map<String, String>> extractData(ResultSet rs) throws SQLException, DataAccessException {
      while (rs.next()) {
        int actionId = rs.getInt("action_id");
        String stateKey = rs.getString("state_key");
        String stateValue = rs.getString("state_value");
        if (!actionStates.containsKey(actionId)) {
          actionStates.put(actionId, new LinkedHashMap<String, String>());
        }
        Map<String, String> stateMap = actionStates.get(actionId);
        stateMap.put(stateKey, stateValue);
      }
      return actionStates;
    }
  }

  public Map<String, StateExecutionStatistics> getStateExecutionStatistics(String type, DateTime createdAfter,
      DateTime createdBefore, DateTime modifiedAfter, DateTime modifiedBefore) {
    final Map<String, StateExecutionStatistics> statistics = new LinkedHashMap<>();
    String executorGroup = executorInfo.getExecutorGroup();
    List<Object> argsList = new ArrayList<>();
    argsList.addAll(asList(executorGroup, type));
    StringBuilder queryBuilder = new StringBuilder(GET_STATISTICS_PREFIX);
    if (createdAfter != null) {
      queryBuilder.append(" and created >= ?");
      argsList.add(createdAfter.toDate());
    }
    if (createdBefore != null) {
      queryBuilder.append(" and created < ?");
      argsList.add(createdBefore.toDate());
    }
    if (modifiedAfter != null) {
      queryBuilder.append(" and modified >= ?");
      argsList.add(modifiedAfter.toDate());
    }
    if (modifiedBefore != null) {
      queryBuilder.append(" and modified < ?");
      argsList.add(modifiedBefore.toDate());
    }
    String query = queryBuilder.append(" and %s").toString();
    Object[] args = argsList.toArray(new Object[argsList.size()]);
    jdbc.query(format(query, "executor_id is not null group by state"), args, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        String state = rs.getString("state");
        getStatisticsForState(statistics, state).executing = rs.getLong("amount");
      }});
    jdbc.query(format(query, "next_activation > current_timestamp group by state"), args, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        String state = rs.getString("state");
        getStatisticsForState(statistics, state).sleeping = rs.getLong("amount");
      }});
    jdbc.query(format(query, "executor_id is null and next_activation <= current_timestamp group by state"), args, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        String state = rs.getString("state");
        getStatisticsForState(statistics, state).queued = rs.getLong("amount");
      }});
    jdbc.query(format(query, "next_activation is null group by state"), args, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        String state = rs.getString("state");
        getStatisticsForState(statistics, state).nonScheduled = rs.getLong("amount");
      }});
    return statistics;
  }

  protected StateExecutionStatistics getStatisticsForState(Map<String, StateExecutionStatistics> statistics, String state) {
    StateExecutionStatistics stats = statistics.get(state);
    if (stats == null) {
      stats = new StateExecutionStatistics();
      statistics.put(state, stats);
    }
    return stats;
  }
}
