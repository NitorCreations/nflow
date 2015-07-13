package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.firstColumnLengthExtractor;
import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.getInt;
import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static com.nitorcreations.nflow.engine.internal.dao.DaoUtil.toTimestamp;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
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
import java.sql.Statement;
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
import org.springframework.util.Assert;

import com.nitorcreations.nflow.engine.internal.config.NFlow;
import com.nitorcreations.nflow.engine.internal.storage.db.SQLVariants;
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
    Object[] instanceValues = new Object[] { instance.type, instance.rootWorkflowId, instance.parentWorkflowId,
        instance.parentActionId, instance.businessKey, instance.externalId, executorInfo.getExecutorGroup(),
        instance.status.name(), instance.state, abbreviate(instance.stateText, instanceStateTextLength),
        toTimestamp(instance.nextActivation) };
    int pos = instanceValues.length;
    Object[] args = Arrays.copyOf(instanceValues, pos + instance.stateVariables.size() * 2);
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
    return "insert into nflow_workflow(type, root_workflow_id, parent_workflow_id, parent_action_id, business_key, external_id, "
        + "executor_group, status, state, state_text, next_activation) values (?, ?, ?, ?, ?, ?, ?, " + sqlVariants.workflowStatus()
        + ", ?, ?, ?)";
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
            ps.setObject(p++, instance.rootWorkflowId);
            ps.setObject(p++, instance.parentWorkflowId);
            ps.setObject(p++, instance.parentActionId);
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
    boolean unknownResults = false;
    for (int i = 0; i < updateStatus.length; ++i) {
      if (updateStatus[i] == Statement.SUCCESS_NO_INFO) {
        unknownResults = true;
        continue;
      }
      if (updateStatus[i] == Statement.EXECUTE_FAILED) {
        throw new IllegalStateException("Failed to insert/update state variables");
      }
      updatedRows += updateStatus[i];
    }
    if (!unknownResults && updatedRows != changedStateVariables.size()) {
      throw new IllegalStateException("Failed to insert/update state variables, expected update count "
          + changedStateVariables.size() + ", actual " + updatedRows);
    }
  }

  @SuppressWarnings("null")
  public void updateWorkflowInstanceAfterExecution(WorkflowInstance instance, WorkflowInstanceAction action,
      List<WorkflowInstance> childWorkflows) {
    Assert.isTrue(action != null, "action can not be null");
    Assert.isTrue(childWorkflows != null, "childWorkflows can not be null");
    if (sqlVariants.hasUpdateableCTE() && childWorkflows.isEmpty()) {
      updateWorkflowInstanceWithCTE(instance, action);
    } else {
      updateWorkflowInstanceWithTransaction(instance, action, childWorkflows);
    }
  }

  public int updateWorkflowInstance(WorkflowInstance instance) {
    // using sqlVariants.nextActivationUpdate() requires that nextActivation is used 3 times
    Timestamp nextActivation = toTimestamp(instance.nextActivation);
    return jdbc.update(updateWorkflowInstanceSql(), instance.status.name(), instance.state,
        abbreviate(instance.stateText, instanceStateTextLength), nextActivation, nextActivation, nextActivation,
        instance.status == executing ? executorInfo.getExecutorId() : null, instance.retries, instance.id);
  }

  private void updateWorkflowInstanceWithTransaction(final WorkflowInstance instance, final WorkflowInstanceAction action,
                                                     final List<WorkflowInstance> childWorkflows) {
    transaction.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        updateWorkflowInstance(instance);
        int parentActionId = insertWorkflowInstanceAction(instance, action);
        for (WorkflowInstance childTemplate : childWorkflows) {
          Integer rootWorkflowId = instance.rootWorkflowId == null ? instance.id : instance.rootWorkflowId;
          WorkflowInstance childWorkflow = new WorkflowInstance.Builder(childTemplate).setRootWorkflowId(rootWorkflowId)
              .setParentWorkflowId(instance.id).setParentActionId(parentActionId).build();
          insertWorkflowInstance(childWorkflow);
        }
      }
    });
  }

  @Transactional
  public void recoverWorkflowInstance(final int instanceId, final WorkflowInstanceAction action) {
    int executorId = executorInfo.getExecutorId();
    int updated = jdbc.update(
        "update nflow_workflow set executor_id = null where id = ? and executor_id in (select id from nflow_executor where "
            + executorInfo.getExecutorGroupCondition() + " and id <> " + executorId + " and expires < current_timestamp)",
        instanceId);
    if (updated > 0) {
      insertWorkflowInstanceAction(action);
    }
  }

  private void updateWorkflowInstanceWithCTE(WorkflowInstance instance, final WorkflowInstanceAction action) {
    int executorId = executorInfo.getExecutorId();
    StringBuilder sqlb = new StringBuilder(256);
    sqlb.append("with wf as (").append(updateWorkflowInstanceSql()).append(" returning id), ");
    sqlb.append("act as (").append(insertWorkflowActionSql()).append(" select wf.id, ?, ")
        .append(sqlVariants.actionType()).append(", ?, ?, ?, ?, ? from wf returning id)");
    Map<String, String> changedStateVariables = changedStateVariables(instance.stateVariables, instance.originalStateVariables);

    // using sqlVariants.nextActivationUpdate() requires that nextActivation is added 3 times
    Timestamp nextActivation = toTimestamp(instance.nextActivation);
    Object[] fixedValues = new Object[] { instance.status.name(), instance.state,
        abbreviate(instance.stateText, instanceStateTextLength), nextActivation, nextActivation, nextActivation,
        instance.status == executing ? executorId : null, instance.retries, instance.id, executorId, action.type.name(),
        action.state, abbreviate(action.stateText, actionStateTextLength), action.retryNo, toTimestamp(action.executionStart),
        toTimestamp(action.executionEnd) };
    int pos = fixedValues.length;
    Object[] args = Arrays.copyOf(fixedValues, pos + changedStateVariables.size() * 2);
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
    return "update nflow_workflow set status = " + sqlVariants.workflowStatus() + ", state = ?, state_text = ?, "
        + "next_activation = " + sqlVariants.nextActivationUpdate()
        + ", external_next_activation = null, executor_id = ?, retries = ? where id = ? and executor_id = "
        + executorInfo.getExecutorId();
  }

  public boolean updateNotRunningWorkflowInstance(WorkflowInstance instance) {
    List<String> vars = new ArrayList<>();
    List<Object> args = new ArrayList<>();
    if (instance.state != null) {
      vars.add("state = ?, retries = 0");
      args.add(instance.state);
    }
    if (instance.stateText != null) {
      vars.add("state_text = ?");
      args.add(instance.stateText);
    }
    if (instance.nextActivation != null) {
      vars.add("next_activation = ?");
      args.add(toTimestamp(instance.nextActivation));
    }
    if (instance.status != null) {
      vars.add("status = " + sqlVariants.workflowStatus());
      args.add(instance.status.name());
    }
    String sql = "update nflow_workflow set " + join(vars, ", ") + " where id = ? and executor_id is null";
    args.add(instance.id);
    return jdbc.update(sql, args.toArray()) == 1;
  }

  @Transactional
  public boolean wakeUpWorkflowExternally(int workflowInstanceId) {
    String sql = "update nflow_workflow set next_activation = (case when executor_id is null then "
        + "least(current_timestamp, coalesce(next_activation, current_timestamp)) else next_activation end), "
        + "external_next_activation = current_timestamp where " + executorInfo.getExecutorGroupCondition()
        + " and id = ? and next_activation is not null";
    return jdbc.update(sql, workflowInstanceId) == 1;
  }

  public boolean wakeupWorkflowInstanceIfNotExecuting(long id, String[] expectedStates) {
    StringBuilder sql = new StringBuilder("update nflow_workflow set next_activation = current_timestamp")
        .append(" where id = ? and executor_id is null and status in (").append(sqlVariants.workflowStatus(inProgress))
        .append(", ").append(sqlVariants.workflowStatus(created))
        .append(") and (next_activation is null or next_activation > current_timestamp)");
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
    fillChildWorkflows(instance);
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
    return "update nflow_workflow set executor_id = " + executorInfo.getExecutorId() + ", status = "
        + sqlVariants.workflowStatus(executing) + ", " + "external_next_activation = null";
  }

  String whereConditionForInstanceUpdate() {
    return "where executor_id is null and status in (" + sqlVariants.workflowStatus(created) + ", "
        + sqlVariants.workflowStatus(inProgress) + ") and next_activation < current_timestamp and "
        + executorInfo.getExecutorGroupCondition() + " order by next_activation asc";
  }

  private List<Integer> pollNextWorkflowInstanceIdsWithUpdateReturning(int batchSize) {
    String sql = updateInstanceForExecutionQuery() + " where id in ("
        + sqlVariants.limit("select id from nflow_workflow " + whereConditionForInstanceUpdate(), Integer.toString(batchSize))
        + ") and executor_id is null returning id";
    return jdbc.queryForList(sql, Integer.class);
  }

  private List<Integer> pollNextWorkflowInstanceIdsWithTransaction(final int batchSize) {
    return transaction.execute(new TransactionCallback<List<Integer>>() {
      @Override
      public List<Integer> doInTransaction(TransactionStatus transactionStatus) {
        String sql = sqlVariants.limit("select id, modified from nflow_workflow " + whereConditionForInstanceUpdate(),
            Integer.toString(batchSize));
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
          if (status != 1 && status != Statement.SUCCESS_NO_INFO) {
            throw new PollingRaceConditionException("Race condition in polling workflow instances detected. "
                + "Multiple pollers using same name (" + executorInfo.getExecutorGroup() + ")");
          }
        }
        return ids;
      }
    });
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
    if (query.parentWorkflowId != null) {
      conditions.add("w.parent_workflow_id = :parent_workflow_id");
      params.addValue("parent_workflow_id", query.parentWorkflowId);
    }
    if (query.parentActionId != null) {
      conditions.add("w.parent_action_id = :parent_action_id");
      params.addValue("parent_action_id", query.parentActionId);
    }
    if (!isEmpty(query.states)) {
      conditions.add("w.state in (:states)");
      params.addValue("states", query.states);
    }
    if (!isEmpty(query.statuses)) {
      List<String> convertedStatuses = new ArrayList<>();
      for (WorkflowInstanceStatus s : query.statuses) {
        convertedStatuses.add(s.name());
      }
      conditions.add("w.status" + sqlVariants.castToText() + " in (:statuses)");
      params.addValue("statuses", convertedStatuses);
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
    sql = sqlVariants.limit(sql, ":limit");
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
    if (query.includeChildWorkflows) {
      for (final WorkflowInstance instance : ret) {
        fillChildWorkflows(instance);
      }
    }
    return ret;
  }

  private void fillChildWorkflows(final WorkflowInstance instance) {
    jdbc.query("select parent_action_id, id from nflow_workflow where parent_workflow_id = ?", new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        int parentActionId = rs.getInt(1);
        int childWorkflowInstanceId = rs.getInt(2);
        List<Integer> children = instance.childWorkflows.get(parentActionId);
        if (children == null) {
          children = new ArrayList<>();
          instance.childWorkflows.put(parentActionId, children);
        }
        children.add(childWorkflowInstanceId);
      }
    }, instance.id);
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
  public int insertWorkflowInstanceAction(final WorkflowInstance instance, final WorkflowInstanceAction action) {
    int actionId = insertWorkflowInstanceAction(action);
    insertVariables(action.workflowInstanceId, actionId, instance.stateVariables, instance.originalStateVariables);
    return actionId;
  }

  @SuppressFBWarnings(value="SIC_INNER_SHOULD_BE_STATIC_ANON", justification="common jdbctemplate practice")
  public int insertWorkflowInstanceAction(final WorkflowInstanceAction action) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressFBWarnings(value="OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE", justification="findbugs does not trust jdbctemplate")
      public PreparedStatement createPreparedStatement(Connection con)
          throws SQLException {
        PreparedStatement p = con.prepareStatement(insertWorkflowActionSql() + " values (?, ?, " + sqlVariants.actionType()
            + ", ?, ?, ?, ?, ?)", new String[] { "id" });
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
      return new WorkflowInstance.Builder()
        .setId(rs.getInt("id"))
        .setExecutorId(getInt(rs, "executor_id"))
        .setRootWorkflowId(getInt(rs, "root_workflow_id"))
        .setParentWorkflowId(getInt(rs, "parent_workflow_id"))
        .setParentActionId(getInt(rs, "parent_action_id"))
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
        .setId(rs.getInt("id"))
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
}
