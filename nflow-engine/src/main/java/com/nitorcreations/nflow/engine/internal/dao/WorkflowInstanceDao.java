package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.toTimestamp;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.left;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.collectionToDelimitedString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.commons.lang3.mutable.MutableInt;
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
import org.springframework.transaction.annotation.Transactional;

import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.engine.internal.storage.db.SQLVariants;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecutionStatistics;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Use setter injection because constructor injection may not work when nFlow is
 * used in some legacy systems.
 */
@Component
public class WorkflowInstanceDao {

  static final Map<String, String> EMPTY_STATE_MAP = Collections.<String,String>emptyMap();
  static final Map<Integer, Map<String, String>> EMPTY_ACTION_STATE_MAP = Collections.<Integer, Map<String, String>>emptyMap();

  // TODO: fetch text field max sizes from database meta data
  private static final int STATE_TEXT_LENGTH = 128;
  private static final String GET_STATISTICS_PREFIX = "select state, count(1) as amount from nflow_workflow where executor_group = ? and type = ?";

  private JdbcTemplate jdbc;
  private NamedParameterJdbcTemplate namedJdbc;
  ExecutorDao executorInfo;
  private SQLVariants sqlVariants;
  private long workflowInstanceQueryMaxResults;
  private long workflowInstanceQueryMaxResultsDefault;

  @Inject
  public void setSQLVariants(SQLVariants sqlVariants) {
    this.sqlVariants = sqlVariants;
  }

  @Inject
  public void setJdbcTemplate(@NFlow JdbcTemplate nflowJdbcTemplate) {
    this.jdbc = nflowJdbcTemplate;
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
    workflowInstanceQueryMaxResultsDefault = env.getRequiredProperty("nflow.workflow.instance.query.max.results.default", Long.class);
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
    insertVariables(id, 0, instance.stateVariables, EMPTY_STATE_MAP);
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

  public boolean updateNotRunningWorkflowInstance(long id, String state, DateTime nextActivation) {
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
    String sql = "update nflow_workflow set " + join(vars, ", ") + " where id = ? and executor_id is null";
    args.add(id);
    return jdbc.update(sql, args.toArray()) == 1;
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
  @Transactional
  public List<Integer> pollNextWorkflowInstanceIds(int batchSize) {
    if (sqlVariants.hasUpdateReturning()) {
      return pollNextWorkflowInstanceIdsUsingUpdateReturning(batchSize);
    }
    return pollNextWorkflowInstanceIdsUsingSelectUpdate(batchSize);
  }

  private List<Integer> pollNextWorkflowInstanceIdsUsingUpdateReturning(int batchSize) {
    return jdbc.queryForList("update nflow_workflow set executor_id = " + executorInfo.getExecutorId()
        + " where id in (select id from nflow_workflow where executor_id is null and next_activation < current_timestamp and "
        + executorInfo.getExecutorGroupCondition() + " order by next_activation asc limit " + batchSize
        + ") and executor_id is null returning id", Integer.class);
  }

  private List<Integer> pollNextWorkflowInstanceIdsUsingSelectUpdate(int batchSize) {
    String sql = "select id, modified from nflow_workflow where executor_id is null and next_activation < current_timestamp and "
        + executorInfo.getExecutorGroupCondition() + " order by next_activation asc limit " + batchSize;
    List<OptimisticLockKey> instances = jdbc.query(sql, new RowMapper<OptimisticLockKey>() {
      @Override
      public OptimisticLockKey mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new OptimisticLockKey(rs.getInt("id"), rs.getString("modified"));
      }
    });
    Collections.sort(instances);
    List<Object[]> batchArgs = new ArrayList<>(instances.size());
    List<Integer> ids = new ArrayList<>(instances.size());
    for (OptimisticLockKey instance : instances) {
      batchArgs.add(new Object[] { instance.id, instance.modified });
      ids.add(instance.id);
    }
    int[] updateStatuses = jdbc.batchUpdate("update nflow_workflow set executor_id = " + executorInfo.getExecutorId()
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

  private static class OptimisticLockKey implements Comparable<OptimisticLockKey> {
    public final int id;
    public final String modified;

    public OptimisticLockKey(int id, String string) {
      this.id = id;
      this.modified = string;
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
        p.setString(4, left(action.stateText, STATE_TEXT_LENGTH));
        p.setInt(5, action.retryNo);
        p.setTimestamp(6, toTimestamp(action.executionStart));
        p.setTimestamp(7, toTimestamp(action.executionEnd));
        return p;
      }
    }, keyHolder);
    return keyHolder.getKey().intValue();
  }

  public String getWorkflowInstanceState(int workflowInstanceId) {
    return jdbc.queryForObject("select state from nflow_workflow where id = ?", String.class, workflowInstanceId);
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
        + "executor_id = ?, retries = ? where id = ? and executor_id = ?";

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
      ps.setString(p++, left(instance.stateText, STATE_TEXT_LENGTH));
      ps.setTimestamp(p++, toTimestamp(instance.nextActivation));
      if (!isInsert) {
        ps.setObject(p++, instance.processing ? executorId : null);
        ps.setInt(p++, instance.retries);
        ps.setInt(p++, instance.id);
        ps.setInt(p++, executorId);
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
