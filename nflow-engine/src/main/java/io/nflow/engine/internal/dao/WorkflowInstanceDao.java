package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.DaoUtil.firstColumnLengthExtractor;
import static io.nflow.engine.internal.dao.DaoUtil.getInt;
import static io.nflow.engine.internal.dao.DaoUtil.getLong;
import static io.nflow.engine.internal.dao.DaoUtil.toTimestamp;
import static io.nflow.engine.internal.dao.NflowTable.ACTION;
import static io.nflow.engine.internal.dao.NflowTable.STATE;
import static io.nflow.engine.internal.dao.NflowTable.WORKFLOW;
import static io.nflow.engine.internal.dao.TableType.convertMainToArchive;
import static io.nflow.engine.internal.dao.WorkflowInstanceDao.WorkflowInstanceRowMapper.ALL_WORKFLOW_COLUMNS;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.recovery;
import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.length;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.transaction.annotation.Propagation.MANDATORY;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.collectionToDelimitedString;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.executor.InstanceInfo;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.model.ModelObject;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.workflow.executor.StateVariableValueTooLongException;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Use setter injection because constructor injection may not work when nFlow is used in some legacy systems.
 */
@Component
@SuppressFBWarnings(value = { "SIC_INNER_SHOULD_BE_STATIC_ANON", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" },
    justification = "common jdbctemplate practice, npe is unlikely")
@Singleton
public class WorkflowInstanceDao {

  private static final Logger logger = getLogger(WorkflowInstanceDao.class);
  private final ConcurrentMap<Long, String> workflowTypeByWorkflowIdCache = new ConcurrentHashMap<>();

  final JdbcTemplate jdbc;
  private final NamedParameterJdbcTemplate namedJdbc;
  private final TransactionTemplate transaction;
  final ExecutorDao executorInfo;
  final SQLVariants sqlVariants;
  private final WorkflowInstanceExecutor workflowInstanceExecutor;
  private final long workflowInstanceQueryMaxResults;
  private final long workflowInstanceQueryMaxResultsDefault;
  private final long workflowInstanceQueryMaxActions;
  private final long workflowInstanceQueryMaxActionsDefault;
  private final int workflowInstanceTypeCacheSize;
  private final AtomicBoolean disableBatchUpdates = new AtomicBoolean();
  AtomicInteger instanceStateTextLength = new AtomicInteger();
  AtomicInteger actionStateTextLength = new AtomicInteger();
  AtomicInteger stateVariableValueMaxLength = new AtomicInteger();
  final WorkflowInstanceRowMapper workflowInstanceRowMapper;
  final WorkflowInstanceActionRowMapper workflowInstanceActionRowMapper;

  @Inject
  public WorkflowInstanceDao(SQLVariants sqlVariants, @NFlow JdbcTemplate nflowJdbcTemplate,
      @NFlow TransactionTemplate transactionTemplate, @NFlow NamedParameterJdbcTemplate nflowNamedParameterJdbcTemplate,
      ExecutorDao executorDao, WorkflowInstanceExecutor workflowInstanceExecutor, WorkflowInstanceFactory workflowInstanceFactory,
      Environment env) {

    this.sqlVariants = sqlVariants;
    this.jdbc = nflowJdbcTemplate;
    this.transaction = transactionTemplate;
    this.namedJdbc = nflowNamedParameterJdbcTemplate;
    this.executorInfo = executorDao;
    this.workflowInstanceExecutor = workflowInstanceExecutor;

    this.workflowInstanceRowMapper = new WorkflowInstanceRowMapper(sqlVariants, workflowInstanceFactory);
    this.workflowInstanceActionRowMapper = new WorkflowInstanceActionRowMapper(sqlVariants);

    workflowInstanceQueryMaxResults = env.getRequiredProperty("nflow.workflow.instance.query.max.results", Long.class);
    workflowInstanceQueryMaxResultsDefault = env.getRequiredProperty("nflow.workflow.instance.query.max.results.default",
        Long.class);
    workflowInstanceQueryMaxActions = env.getRequiredProperty("nflow.workflow.instance.query.max.actions", Long.class);
    workflowInstanceQueryMaxActionsDefault = env.getRequiredProperty("nflow.workflow.instance.query.max.actions.default",
        Long.class);
    disableBatchUpdates.set(env.getRequiredProperty("nflow.db.disable_batch_updates", Boolean.class));
    if (disableBatchUpdates.get()) {
      logger.info("nFlow DB batch updates are disabled (system property nflow.db.disable_batch_updates=true)");
    }
    workflowInstanceTypeCacheSize = env.getRequiredProperty("nflow.db.workflowInstanceType.cacheSize", Integer.class);
    instanceStateTextLength.set(env.getProperty("nflow.workflow.instance.state.text.length", Integer.class, -1));
    actionStateTextLength.set(env.getProperty("nflow.workflow.action.state.text.length", Integer.class, -1));
    stateVariableValueMaxLength.set(env.getProperty("nflow.workflow.state.variable.value.length", Integer.class, -1));
  }

  private int getInstanceStateTextLength() {
    return getFieldLength(instanceStateTextLength, "state_text", WORKFLOW, "nflow.workflow.instance.state.text.length");
  }

  private int getActionStateTextLength() {
    return getFieldLength(actionStateTextLength, "state_text", ACTION, "nflow.workflow.action.state.text.length");
  }

  int getStateVariableValueMaxLength() {
    return getFieldLength(stateVariableValueMaxLength, "state_value", STATE, "nflow.workflow.state.variable.value.length");
  }

  private int getFieldLength(AtomicInteger length, String field, NflowTable table, String property) {
    int value = length.get();
    if (value == -1) {
      value = ofNullable(jdbc.query("select " + field + " from " + table.main + " where 1=0", firstColumnLengthExtractor))
          .orElseThrow(() -> new IllegalStateException("Failed to read " + table.main + "." + field
              + " column length from database, please set correct value to " + property));
      length.set(value);
    }
    return value;
  }

  public long insertWorkflowInstance(WorkflowInstance instance) {
    long id;
    if (sqlVariants.hasUpdateableCTE()) {
      id = insertWorkflowInstanceWithCte(instance);
    } else {
      id = insertWorkflowInstanceWithTransaction(instance);
    }
    if (instance.nextActivation != null && instance.nextActivation.isBeforeNow()) {
      workflowInstanceExecutor.wakeUpDispatcherIfNeeded();
    }
    return id;
  }

  private long insertWorkflowInstanceWithCte(WorkflowInstance instance) {
    try {
      StringBuilder sqlb = new StringBuilder(256);
      sqlb.append("with wf as (").append(insertWorkflowInstanceSql()).append(" returning id)");
      Object[] instanceValues = new Object[] { instance.type, instance.priority, instance.parentWorkflowId,
          instance.parentActionId, instance.businessKey, instance.externalId, executorInfo.getExecutorGroup(),
          instance.status.name(), instance.state, abbreviate(instance.stateText, getInstanceStateTextLength()),
          toTimestamp(instance.nextActivation), instance.signal.orElse(null) };
      int pos = instanceValues.length;
      Object[] args = Arrays.copyOf(instanceValues, pos + instance.stateVariables.size() * 2);
      for (Entry<String, String> variable : instance.stateVariables.entrySet()) {
        sqlb.append(", ins").append(pos).append(" as (").append(insertWorkflowInstanceStateSql())
            .append(" select wf.id,0,?,? from wf)");
        args[pos++] = variable.getKey();
        args[pos++] = variable.getValue();
      }
      sqlb.append(" select wf.id from wf");
      return jdbc.queryForObject(sqlb.toString(), Long.class, args);
    } catch (DuplicateKeyException e) {
      logger.warn("Failed to insert workflow instance", e);
      return -1;
    }
  }

  boolean useBatchUpdate() {
    return sqlVariants.useBatchUpdate() && !disableBatchUpdates.get();
  }

  String insertWorkflowInstanceSql() {
    return "insert into nflow_workflow(type, priority, parent_workflow_id, parent_action_id, business_key, external_id, "
        + "executor_group, status, state, state_text, next_activation, workflow_signal) values (?, ?, ?, ?, ?, ?, ?, "
        + sqlVariants.workflowStatus() + ", ?, ?, ?, ?)";
  }

  String insertWorkflowInstanceStateSql() {
    return "insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value)";
  }

  private long insertWorkflowInstanceWithTransaction(final WorkflowInstance instance) {
    return transaction.execute(status -> {
      KeyHolder keyHolder = new GeneratedKeyHolder();
      try {
        jdbc.update(new PreparedStatementCreator() {
          @Override
          @SuppressFBWarnings(value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
              justification = "SQL is practically constant")
          public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
            int p = 1;
            @SuppressWarnings("resource")
            PreparedStatement ps = connection.prepareStatement(insertWorkflowInstanceSql(), new String[] { "id" });
            try {
              ps.setString(p++, instance.type);
              ps.setShort(p++, instance.priority);
              ps.setObject(p++, instance.parentWorkflowId);
              ps.setObject(p++, instance.parentActionId);
              ps.setString(p++, instance.businessKey);
              ps.setString(p++, instance.externalId);
              ps.setString(p++, executorInfo.getExecutorGroup());
              ps.setString(p++, instance.status.name());
              ps.setString(p++, instance.state);
              ps.setString(p++, abbreviate(instance.stateText, getInstanceStateTextLength()));
              sqlVariants.setDateTime(ps, p++, instance.nextActivation);
              if (instance.signal.isPresent()) {
                ps.setInt(p++, instance.signal.get());
              } else {
                ps.setNull(p++, Types.INTEGER);
              }
            } catch (Exception e) {
              ps.close();
              throw e;
            }
            return ps;
          }
        }, keyHolder);
      } catch (DuplicateKeyException e) {
        logger.warn("Failed to insert workflow instance", e);
        return -1L;
      }
      long id = keyHolder.getKey().longValue();
      insertVariables(id, 0, instance.stateVariables);
      return id;
    });
  }

  void insertVariables(final long id, final long actionId, Map<String, String> changedStateVariables) {
    if (changedStateVariables.isEmpty()) {
      return;
    }
    if (useBatchUpdate()) {
      insertVariablesWithBatchUpdate(id, actionId, changedStateVariables);
    } else {
      insertVariablesWithMultipleUpdates(id, actionId, changedStateVariables);
    }
  }

  private void insertVariablesWithMultipleUpdates(final long id, final long actionId, Map<String, String> changedStateVariables) {
    for (Entry<String, String> entry : changedStateVariables.entrySet()) {
      int updated = jdbc.update(insertWorkflowInstanceStateSql() + " values (?,?,?,?)", id, actionId, entry.getKey(),
          entry.getValue());
      if (updated != 1) {
        throw new IllegalStateException("Failed to insert state variable " + entry.getKey());
      }
    }
  }

  private void insertVariablesWithBatchUpdate(final long id, final long actionId, Map<String, String> changedStateVariables) {
    final Iterator<Entry<String, String>> variables = changedStateVariables.entrySet().iterator();
    int[] updateStatus = jdbc.batchUpdate(insertWorkflowInstanceStateSql() + " values (?,?,?,?)",
        new AbstractInterruptibleBatchPreparedStatementSetter() {
          @Override
          protected boolean setValuesIfAvailable(PreparedStatement ps, int i) throws SQLException {
            if (!variables.hasNext()) {
              return false;
            }
            Entry<String, String> variable = variables.next();
            ps.setLong(1, id);
            ps.setLong(2, actionId);
            ps.setString(3, variable.getKey());
            ps.setString(4, variable.getValue());
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
        throw new IllegalStateException("Failed to insert/update state variable at index " + i + " (" + updateStatus[i] + ")");
      }
      updatedRows += updateStatus[i];
    }
    int changedVariables = changedStateVariables.size();
    if (!unknownResults && updatedRows != changedVariables) {
      throw new IllegalStateException(
          "Failed to insert/update state variables, expected update count " + changedVariables + ", actual " + updatedRows);
    }
  }

  @SuppressWarnings("null")
  public void updateWorkflowInstanceAfterExecution(WorkflowInstance instance, WorkflowInstanceAction action,
      List<WorkflowInstance> childWorkflows, List<WorkflowInstance> workflows, boolean createAction) {
    Assert.isTrue(action != null, "action can not be null");
    Assert.isTrue(childWorkflows != null, "childWorkflows can not be null");
    Assert.isTrue(workflows != null, "workflows can not be null");
    Map<String, String> changedStateVariables = instance.getChangedStateVariables();
    if (!createAction && (!childWorkflows.isEmpty() || !workflows.isEmpty() || !changedStateVariables.isEmpty())) {
      logger.info("Forcing action creation because new workflow instances are created or state variables are changed.");
      createAction = true;
    }
    if (createAction) {
      if (sqlVariants.hasUpdateableCTE() && childWorkflows.isEmpty() && workflows.isEmpty()) {
        updateWorkflowInstanceWithCTE(instance, action, changedStateVariables);
      } else {
        updateWorkflowInstanceWithTransaction(instance, action, childWorkflows, workflows, changedStateVariables);
      }
    } else {
      updateWorkflowInstance(instance);
    }
  }

  public int updateWorkflowInstance(WorkflowInstance instance) {
    // using sqlVariants.nextActivationUpdate() requires that nextActivation is used 3 times
    Object nextActivation = sqlVariants.toTimestampObject(instance.nextActivation);
    Object[] params = {
      instance.status.name(), instance.state, abbreviate(instance.stateText, getInstanceStateTextLength()),
      nextActivation, nextActivation, nextActivation,
      instance.status == executing ? executorInfo.getExecutorId() : null, instance.retries, instance.businessKey,
      sqlVariants.toTimestampObject(instance.started), instance.id
    };
    int[] sqlTypes = {
          Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP, Types.TIMESTAMP,
          Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.TIMESTAMP, Types.INTEGER
    };
    int updated = jdbc.update(updateWorkflowInstanceSql(), params, sqlTypes);
    if (updated == 0) {
      logger.warn(
          "Updating workflow instance {} did not update any rows in the database, instance may have been recovered by another executor.",
          instance.id);
    }
    return updated;
  }

  private void updateWorkflowInstanceWithTransaction(final WorkflowInstance instance, final WorkflowInstanceAction action,
      final List<WorkflowInstance> childWorkflows, final List<WorkflowInstance> workflows,
      final Map<String, String> changedStateVariables) {
    transaction.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        int updated = updateWorkflowInstance(instance);
        if (updated == 0) {
          return;
        }
        long parentActionId = insertWorkflowInstanceAction(action);
        insertVariables(action.workflowInstanceId, parentActionId, changedStateVariables);
        for (WorkflowInstance childTemplate : childWorkflows) {
          WorkflowInstance childWorkflow = new WorkflowInstance.Builder(childTemplate).setParentWorkflowId(instance.id)
              .setParentActionId(parentActionId).build();
          insertWorkflowInstance(childWorkflow);
        }
        for (WorkflowInstance workflow : workflows) {
          insertWorkflowInstance(workflow);
        }
      }
    });
  }

  public void recoverWorkflowInstancesFromDeadNodes() {
    var recoverableExecutorIds = executorInfo.getRecoverableExecutorIds();
    if (recoverableExecutorIds.isEmpty()) {
      return;
    }
    WorkflowInstanceAction.Builder builder = new WorkflowInstanceAction.Builder().setExecutionStart(now()).setExecutionEnd(now())
        .setType(recovery).setStateText("Recovered");
    for (InstanceInfo instance : getRecoverableWorkflowInstances(recoverableExecutorIds)) {
      WorkflowInstanceAction action = builder.setState(instance.state()).setWorkflowInstanceId(instance.id()).build();
      recoverWorkflowInstance(instance.id(), instance.executorId(), action);
    }
    recoverableExecutorIds.forEach(executorInfo::markRecovered);
  }

  private List<InstanceInfo> getRecoverableWorkflowInstances(Collection<Integer> executorsIds) {
    StringBuilder sql = new StringBuilder(128);
    sql.append("select id, executor_id, state from nflow_workflow where executor_id in (");
    executorsIds.forEach(id -> sql.append("?,"));
    sql.setCharAt(sql.length() - 1, ')');
    return jdbc.query(sql.toString(), (rs, rowNum) -> new InstanceInfo(rs.getLong(1), rs.getInt(2), rs.getString(3)),
      (Object[]) executorsIds.toArray(new Integer[0]));
  }

  private void recoverWorkflowInstance(final long instanceId, int expectedExecutorId, final WorkflowInstanceAction action) {
    transaction.execute(status -> {
      int updated = jdbc.update("update nflow_workflow set executor_id = null, status = "
          + sqlVariants.workflowStatus(inProgress) + " where id = ? and executor_id = ?",
              instanceId, expectedExecutorId);
      if (updated > 0) {
        insertWorkflowInstanceAction(action);
      }
      return null;
    });
  }

  private void updateWorkflowInstanceWithCTE(WorkflowInstance instance, final WorkflowInstanceAction action,
      Map<String, String> changedStateVariables) {
    int executorId = executorInfo.getExecutorId();
    StringBuilder sqlb = new StringBuilder(256);
    sqlb.append("with wf as (").append(updateWorkflowInstanceSql()).append(" returning id), ");
    sqlb.append("act as (").append(insertWorkflowActionSql()).append(" select wf.id, ?, ").append(sqlVariants.actionType())
        .append(", ?, ?, ?, ?, ? from wf returning id)");

    // using sqlVariants.nextActivationUpdate() requires that nextActivation is added 3 times
    Timestamp nextActivation = toTimestamp(instance.nextActivation);
    Object[] fixedValues = new Object[] { instance.status.name(), instance.state,
        abbreviate(instance.stateText, getInstanceStateTextLength()), nextActivation, nextActivation, nextActivation,
        instance.status == executing ? executorId : null, instance.retries, instance.businessKey,
        toTimestamp(action.executionStart), instance.id, executorId, action.type.name(), action.state,
        abbreviate(action.stateText, getActionStateTextLength()), action.retryNo, toTimestamp(action.executionStart),
        toTimestamp(action.executionEnd) };
    int pos = fixedValues.length;
    Object[] args = Arrays.copyOf(fixedValues, pos + changedStateVariables.size() * 2);
    for (Entry<String, String> variable : changedStateVariables.entrySet()) {
      sqlb.append(", ins").append(pos).append(" as (").append(insertWorkflowInstanceStateSql())
          .append(" select wf.id,act.id,?,? from wf,act)");
      args[pos++] = variable.getKey();
      args[pos++] = variable.getValue();
    }
    sqlb.append(" select act.id from act");
    var result = jdbc.queryForObject(sqlb.toString(), Long.class, args);
    if (result == null) {
      logger.warn("Updating workflow instance {} returned null, instance may have been recovered by another executor.",
          instance.id);
    }
  }

  public void checkStateVariableValueLength(String name, String value) {
    int maxLength = getStateVariableValueMaxLength();
    if (length(value) > maxLength) {
      throw new StateVariableValueTooLongException("Too long value (length = " + length(value) + ") for state variable " + name
          + ": maximum allowed length is " + maxLength);
    }
  }

  String insertWorkflowActionSql() {
    return "insert into nflow_workflow_action(workflow_id, executor_id, type, state, state_text, retry_no, execution_start, execution_end)";
  }

  private String updateWorkflowInstanceSql() {
    return "update nflow_workflow set status = " + sqlVariants.workflowStatus() + ", state = ?, state_text = ?, "
        + "next_activation = " + sqlVariants.nextActivationUpdate()
        + ", external_next_activation = null, executor_id = ?, retries = ?, business_key = ?, "
        + "started = (case when started is null then ? else started end) where id = ? and executor_id = "
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
      args.add(sqlVariants.toTimestampObject(instance.nextActivation));
    }
    if (instance.status != null) {
      vars.add("status = " + sqlVariants.workflowStatus());
      args.add(instance.status.name());
    }
    if (instance.businessKey != null) {
      vars.add("business_key = ?");
      args.add(instance.businessKey);
    }
    String sql = "update nflow_workflow set " + join(vars, ", ") + " where id = ? and executor_id is null";
    args.add(instance.id);
    return jdbc.update(sql, args.toArray()) == 1;
  }

  public boolean wakeUpWorkflowExternally(long workflowInstanceId, List<String> expectedStates) {
    StringBuilder sql = new StringBuilder("update nflow_workflow set next_activation = (case when executor_id is null then ")
        .append("case when ").append(sqlVariants.dateLtEqDiff("next_activation", "current_timestamp"))
        .append(" then next_activation else current_timestamp end else next_activation end), ")
        .append("external_next_activation = current_timestamp where ").append(executorInfo.getExecutorGroupCondition())
        .append(" and id = ? and next_activation is not null");
    return addExpectedStatesToQueryAndUpdate(sql, workflowInstanceId, expectedStates);
  }

  public boolean wakeupWorkflowInstanceIfNotExecuting(long workflowInstanceId, List<String> expectedStates) {
    StringBuilder sql = new StringBuilder("update nflow_workflow set next_activation = current_timestamp")
        .append(" where id = ? and executor_id is null and status in (").append(sqlVariants.workflowStatus(inProgress))
        .append(", ").append(sqlVariants.workflowStatus(created))
        .append(") and (next_activation is null or next_activation > current_timestamp)");
    return addExpectedStatesToQueryAndUpdate(sql, workflowInstanceId, expectedStates);
  }

  private boolean addExpectedStatesToQueryAndUpdate(StringBuilder sql, long workflowInstanceId, List<String> expectedStates) {
    Object[] args = new Object[1 + expectedStates.size()];
    args[0] = workflowInstanceId;
    if (!expectedStates.isEmpty()) {
      sql.append(" and state in (");
      for (int i = 0; i < expectedStates.size(); i++) {
        sql.append("?,");
        args[i + 1] = expectedStates.get(i);
      }
      sql.setCharAt(sql.length() - 1, ')');
    }
    return jdbc.update(sql.toString(), args) == 1;
  }

  public WorkflowInstance getWorkflowInstance(long id, Set<WorkflowInstanceInclude> includes, Long maxActions,
      boolean queryArchive) {
    String sql = "select " + ALL_WORKFLOW_COLUMNS + ", 0 as archived from " + WORKFLOW.main + " where id = ?";
    Object[] args = new Object[] { id };
    if (queryArchive) {
      sql += " union all select " + ALL_WORKFLOW_COLUMNS + ", 1 as archived from " + WORKFLOW.archive + " where id = ?";
      args = new Object[] { id, id };
    }
    WorkflowInstance instance = jdbc.queryForObject(sql, workflowInstanceRowMapper, args).build();
    if (includes.contains(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES)) {
      fillState(instance);
    }
    if (includes.contains(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS)) {
      fillChildWorkflowIds(instance, queryArchive);
    }
    if (includes.contains(WorkflowInstanceInclude.ACTIONS)) {
      fillActions(instance, includes.contains(WorkflowInstanceInclude.ACTION_STATE_VARIABLES), maxActions);
    }
    return instance;
  }

  private void fillState(final WorkflowInstance instance) {
    String tableName = STATE.tableFor(instance);
    jdbc.query("select outside.state_key, outside.state_value from " + tableName + " outside inner join "
        + "(select workflow_id, max(action_id) action_id, state_key from " + tableName
        + " where workflow_id = ? group by workflow_id, state_key) inside "
        + "on outside.workflow_id = inside.workflow_id and outside.action_id = inside.action_id and outside.state_key = inside.state_key",
        rs -> {
          instance.stateVariables.put(rs.getString(1), rs.getString(2));
        }, instance.id);
    instance.originalStateVariables.putAll(instance.stateVariables);
  }

  public List<Long> pollNextWorkflowInstanceIds(final int batchSize) {
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
        + sqlVariants.workflowStatus(inProgress) + ") and " + sqlVariants.dateLtEqDiff("next_activation", "current_timestamp")
        + " and " + executorInfo.getExecutorGroupCondition() + " order by priority desc, next_activation asc";
  }

  private List<Long> pollNextWorkflowInstanceIdsWithUpdateReturning(int batchSize) {
    String sql = updateInstanceForExecutionQuery() + " where id in ("
        + sqlVariants.limit(
            "select id from nflow_workflow " + sqlVariants.withUpdateSkipLocked() + whereConditionForInstanceUpdate(), batchSize)
        + sqlVariants.forUpdateSkipLocked() + ") and executor_id is null returning id";
    List<Long> ids = jdbc.queryForList(sql, Long.class);
    if (ids.size() > batchSize) {
      // very rare, occurs only on empty postgresql database where statistics have not yet been updated
      // https://github.com/feikesteenbergen/demos/blob/master/bugs/update_from_correlated.adoc
      // better just clear the executors_id a few times rather than make the original query more complex
      logger.warn("Got too many workflow instances {} > {}", ids.size(), batchSize);
      List<Long> extras = ids.subList(batchSize, ids.size());
      clearExecutorId(extras);
      ids = ids.subList(0, batchSize);
    }
    return ids;
  }

  public void clearExecutorId(List<Long> workflowInstances) {
    jdbc.update("update nflow_workflow set executor_id=null, status = "
            + sqlVariants.workflowStatus(inProgress) + " where executor_id = " + executorInfo.getExecutorId() +
            " and id in (" + workflowInstances.stream().map(String::valueOf).collect(joining(",")) + ")");
  }

  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "PollingRaceConditionException message is ok")
  private List<Long> pollNextWorkflowInstanceIdsWithTransaction(final int batchSize) {
    String sql = sqlVariants.limit("select id, modified from nflow_workflow " + whereConditionForInstanceUpdate(), batchSize);
    List<OptimisticLockKey> instances = transaction.execute(
        tx -> jdbc.query(sql, (rs, rowNum) -> new OptimisticLockKey(rs.getLong("id"), sqlVariants.getTimestamp(rs, "modified"))));
    if (instances.isEmpty()) {
      return emptyList();
    }
    sort(instances);
    List<Long> ids = transaction.execute(transactionStatus -> {
      if (useBatchUpdate()) {
        return updateNextWorkflowInstancesWithBatchUpdate(instances);
      }
      return updateNextWorkflowInstancesWithMultipleUpdates(instances);
    });
    if (ids.isEmpty()) {
      throw new PollingRaceConditionException(
          "None of the workflow instances selected for processing were successfully reserved for this executor, trying again later.");
    }
    return ids;
  }

  private List<Long> updateNextWorkflowInstancesWithMultipleUpdates(Collection<OptimisticLockKey> instances) {
    String sql = updateInstanceForExecutionQuery() + " where id = ? and modified = ? and executor_id is null";
    return instances.stream()
        .flatMap(instance -> jdbc.update(sql, instance.id, sqlVariants.tuneTimestampForDb(instance.modified)) == 1
            ? Stream.of(instance.id)
            : empty())
        .collect(toList());
  }

  @SuppressFBWarnings(value = "WEM_WEAK_EXCEPTION_MESSAGING", justification = "PollingBatchException message is ok")
  private List<Long> updateNextWorkflowInstancesWithBatchUpdate(List<OptimisticLockKey> instances) {
    String sql = updateInstanceForExecutionQuery() + " where id = ? and modified = ? and executor_id is null";
    List<Object[]> batchArgs = instances.stream()
        .map(instance -> new Object[] { instance.id, sqlVariants.tuneTimestampForDb(instance.modified) }).collect(toList());
    int[] updateStatuses = jdbc.batchUpdate(sql, batchArgs);
    List<Long> ids = new ArrayList<>(instances.size());
    for (int i = 0; i < updateStatuses.length; ++i) {
      int status = updateStatuses[i];
      if (status == 1) {
        ids.add(instances.get(i).id);
      } else if (status != 0) {
        disableBatchUpdates.set(true);
        throw new PollingBatchException(
            "Database was unable to provide information about affected rows in a batch update. Disabling batch updates.");
      }
    }
    return ids;
  }

  private static class OptimisticLockKey extends ModelObject implements Comparable<OptimisticLockKey> {
    public final long id;
    public final Object modified;

    public OptimisticLockKey(long id, Object modified) {
      this.id = id;
      this.modified = modified;
    }

    @Override
    public int compareTo(OptimisticLockKey other) {
      return Long.compare(this.id, other.id);
    }
  }

  public List<WorkflowInstance> queryWorkflowInstances(QueryWorkflowInstances query) {
    return queryWorkflowInstancesAsStream(query).collect(toList());
  }

  public Stream<WorkflowInstance> queryWorkflowInstancesAsStream(QueryWorkflowInstances query) {
    List<String> conditions = new ArrayList<>();
    MapSqlParameterSource params = new MapSqlParameterSource();
    queryOptionsToSqlAndParams(query, conditions, params);
    conditions.add(executorInfo.getExecutorGroupCondition());
    String sqlSuffix = "from nflow_workflow wf ";
    if (query.stateVariableKey != null) {
      sqlSuffix += "inner join nflow_workflow_state wfs on wf.id = wfs.workflow_id and wfs.state_key = :state_key and " + sqlVariants.clobToComparable("wfs.state_value") + " = :state_value ";
      conditions.add(
          "wfs.action_id = (select max(action_id) from nflow_workflow_state where workflow_id = wf.id and state_key = :state_key)");
      params.addValue("state_key", query.stateVariableKey);
      params.addValue("state_value", query.stateVariableValue);
    }
    sqlSuffix += "where " + collectionToDelimitedString(conditions, " and ") + " order by id desc";
    long maxResults = getMaxResults(query.maxResults);
    String sql = sqlVariants.limit("select " + ALL_WORKFLOW_COLUMNS + ", 0 as archived " + sqlSuffix, maxResults);
    List<WorkflowInstance.Builder> results = namedJdbc.query(sql, params, workflowInstanceRowMapper);
    Stream<WorkflowInstance.Builder> resultStream = results.stream();
    // calculate how many results to try to search from archive
    maxResults -= results.size();
    if (query.queryArchive && maxResults > 0) {
      sql = sqlVariants.limit("select " + ALL_WORKFLOW_COLUMNS + ", 1 as archived " + convertMainToArchive(sqlSuffix), maxResults);
      resultStream = concat(resultStream, namedJdbc.query(sql, params, workflowInstanceRowMapper).stream());
    }
    Stream<WorkflowInstance> ret = resultStream.map(WorkflowInstance.Builder::build);
    if (query.includeCurrentStateVariables) {
      ret = ret.peek(instance -> fillState(instance));
    }
    if (query.includeActions) {
      ret = ret.peek(instance -> fillActions(instance, query.includeActionStateVariables, query.maxActions));
    }
    if (query.includeChildWorkflows) {
      ret = ret.peek(instance -> fillChildWorkflowIds(instance, query.queryArchive));
    }
    return ret;
  }

  @SuppressFBWarnings(value = "STT_STRING_PARSING_A_FIELD", justification = "businessKey and externalId are strings")
  private void queryOptionsToSqlAndParams(QueryWorkflowInstances query, List<String> conditions, MapSqlParameterSource params) {
    if (!isEmpty(query.ids)) {
      if (query.ids.size() == 1) {
        conditions.add("id = :id");
        params.addValue("id", query.ids.get(0));
      } else {
        conditions.add("id in (:ids)");
        params.addValue("ids", query.ids);
      }
    }
    if (!isEmpty(query.types)) {
      if (query.types.size() == 1) {
        conditions.add("type = :type");
        params.addValue("type", query.types.get(0));
      } else {
        conditions.add("type in (:types)");
        params.addValue("types", query.types);
      }
    }
    if (query.parentWorkflowId != null) {
      conditions.add("parent_workflow_id = :parent_workflow_id");
      params.addValue("parent_workflow_id", query.parentWorkflowId);
    }
    if (query.parentActionId != null) {
      conditions.add("parent_action_id = :parent_action_id");
      params.addValue("parent_action_id", query.parentActionId);
    }
    if (!isEmpty(query.states)) {
      if (query.states.size() == 1) {
        conditions.add("state = :state");
        params.addValue("state", query.states.get(0));
      } else {
        conditions.add("state in (:states)");
        params.addValue("states", query.states);
      }
    }
    if (!isEmpty(query.statuses)) {
      List<String> convertedStatuses = query.statuses.stream().map(WorkflowInstanceStatus::name).collect(toList());
      conditions.add("status" + sqlVariants.castToText() + " in (:statuses)");
      params.addValue("statuses", convertedStatuses);
    }
    if (query.businessKey != null) {
      if (query.businessKey.indexOf('%') >= 0) {
        conditions.add("business_key " + sqlVariants.caseSensitiveLike() + " :business_key");
      } else {
        conditions.add("business_key = :business_key");
      }
      params.addValue("business_key", query.businessKey);
    }
    if (query.externalId != null) {
      if (query.externalId.indexOf('%') >= 0) {
        conditions.add("external_id " + sqlVariants.caseSensitiveLike() + " :external_id");
      } else {
        conditions.add("external_id = :external_id");
      }
      params.addValue("external_id", query.externalId);
    }
  }

  private void fillChildWorkflowIds(final WorkflowInstance instance, boolean queryArchive) {
    Stream<String> tables = queryArchive ? Stream.of(WORKFLOW.main, WORKFLOW.archive) : Stream.of(WORKFLOW.main);
    String sql = tables.map(table -> "select parent_action_id, id from " + table + " where parent_workflow_id = ?")
        .collect(joining(" union all "));
    Object[] args = queryArchive ? new Object[] { instance.id, instance.id } : new Object[] { instance.id };
    jdbc.query(sql, rs -> {
      long parentActionId = rs.getLong(1);
      long childWorkflowInstanceId = rs.getLong(2);
      List<Long> children = instance.childWorkflows.computeIfAbsent(parentActionId, k -> new ArrayList<>());
      children.add(childWorkflowInstanceId);
    }, args);
  }

  private long getMaxResults(Long maxResults) {
    if (maxResults == null) {
      return workflowInstanceQueryMaxResultsDefault;
    }
    return min(maxResults, workflowInstanceQueryMaxResults);
  }

  private void fillActions(WorkflowInstance instance, boolean includeStateVariables, Long requestedMaxActions) {
    long maxActions = getMaxActions(requestedMaxActions);
    String tableName = ACTION.tableFor(instance);
    String sql = sqlVariants
        .limit("select * from " + tableName + " where workflow_id = ? order by id desc", maxActions);
    List<WorkflowInstanceAction.Builder> actionBuilders = jdbc.query(sql, workflowInstanceActionRowMapper,
        instance.id);
    if (includeStateVariables) {
      Map<Long, Map<String, String>> actionStates = fetchActionStateVariables(instance, actionBuilders.size(), maxActions);
      actionBuilders.forEach(builder -> {
        Map<String, String> actionState = actionStates.get(builder.getId());
        if (actionState != null) {
          builder.setUpdatedStateVariables(actionState);
        }
      });
    }
    actionBuilders.stream().map(WorkflowInstanceAction.Builder::build).forEach(instance.actions::add);
  }

  private long getMaxActions(Long maxActions) {
    if (maxActions == null) {
      return workflowInstanceQueryMaxActionsDefault;
    }
    return min(maxActions, workflowInstanceQueryMaxActions);
  }

  private Map<Long, Map<String, String>> fetchActionStateVariables(WorkflowInstance instance, long actions, long maxActions) {
    String stateTableName = STATE.tableFor(instance);
    String actionTableName = ACTION.tableFor(instance);
    if (actions < maxActions) {
      return jdbc.query("select * from " + stateTableName + " where workflow_id = ? order by action_id, state_key asc",
          new WorkflowActionStateRowMapper(), instance.id);
    }
    return jdbc.query("select nflow_workflow_state.* from ("
        + sqlVariants.limit("select id from " + actionTableName + " nflow_workflow_action where workflow_id = ? order by id desc",
            maxActions)
        + ") action_id inner join " + stateTableName
        + " nflow_workflow_state on nflow_workflow_state.workflow_id = ? and action_id.id = nflow_workflow_state.action_id "
        + "order by nflow_workflow_state.action_id, nflow_workflow_state.state_key asc", new WorkflowActionStateRowMapper(),
        instance.id, instance.id);
  }

  @Transactional(propagation = MANDATORY)
  public long insertWorkflowInstanceAction(final WorkflowInstance instance, final WorkflowInstanceAction action) {
    long actionId = insertWorkflowInstanceAction(action);
    insertVariables(action.workflowInstanceId, actionId, instance.getChangedStateVariables());
    return actionId;
  }

  public long insertWorkflowInstanceAction(final WorkflowInstanceAction action) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressFBWarnings(value = { "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE",
          "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" },
          justification = "findbugs does not trust jdbctemplate, sql string is practically constant")
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement p = con.prepareStatement(
            insertWorkflowActionSql() + " values (?, ?, " + sqlVariants.actionType() + ", ?, ?, ?, ?, ?)", new String[] { "id" });
        int field = 1;
        p.setLong(field++, action.workflowInstanceId);
        p.setInt(field++, executorInfo.getExecutorId());
        p.setString(field++, action.type.name());
        p.setString(field++, action.state);
        p.setString(field++, abbreviate(action.stateText, getActionStateTextLength()));
        p.setInt(field++, action.retryNo);
        sqlVariants.setDateTime(p, field++, action.executionStart);
        sqlVariants.setDateTime(p, field++, action.executionEnd);
        return p;
      }
    }, keyHolder);
    return keyHolder.getKey().longValue();
  }

  public String getWorkflowInstanceState(long workflowInstanceId) {
    return jdbc.queryForObject("select state from nflow_workflow where id = ?", String.class, workflowInstanceId);
  }

  static class WorkflowInstanceRowMapper implements RowMapper<WorkflowInstance.Builder> {
    static final String ALL_WORKFLOW_COLUMNS = "id, executor_id, parent_workflow_id, parent_action_id, status, type, priority, business_key, external_id, " +
            "state, state_text, next_activation, retries, created, modified, started, executor_group, workflow_signal";

    private final SQLVariants sqlVariants;
    private final WorkflowInstanceFactory workflowInstanceFactory;

    public WorkflowInstanceRowMapper(SQLVariants sqlVariants, WorkflowInstanceFactory workflowInstanceFactory) {
      this.sqlVariants = sqlVariants;
      this.workflowInstanceFactory = workflowInstanceFactory;
    }

    @Override
    public WorkflowInstance.Builder mapRow(ResultSet rs, int rowNum) throws SQLException {
      return workflowInstanceFactory.newWorkflowInstanceBuilder()
          .setId(rs.getLong("id"))
          .setExecutorId(getInt(rs, "executor_id"))
          .setParentWorkflowId(getLong(rs, "parent_workflow_id"))
          .setParentActionId(getLong(rs, "parent_action_id"))
          .setStatus(WorkflowInstanceStatus.valueOf(rs.getString("status")))
          .setType(rs.getString("type"))
          .setPriority(rs.getShort("priority"))
          .setBusinessKey(rs.getString("business_key"))
          .setExternalId(rs.getString("external_id"))
          .setState(rs.getString("state"))
          .setStateText(rs.getString("state_text"))
          .setActions(new ArrayList<>())
          .setNextActivation(sqlVariants.getDateTime(rs, "next_activation"))
          .setRetries(rs.getInt("retries"))
          .setCreated(sqlVariants.getDateTime(rs, "created"))
          .setModified(sqlVariants.getDateTime(rs, "modified"))
          .setStartedIfNotSet(sqlVariants.getDateTime(rs, "started"))
          .setExecutorGroup(rs.getString("executor_group"))
          .setSignal(ofNullable(getInt(rs, "workflow_signal")))
          .setArchived(rs.getBoolean("archived"));
    }
  }

  static class WorkflowInstanceActionRowMapper implements RowMapper<WorkflowInstanceAction.Builder> {
    private final SQLVariants sqlVariants;

    public WorkflowInstanceActionRowMapper(SQLVariants sqlVariants) {
      this.sqlVariants = sqlVariants;
    }

    @Override
    public WorkflowInstanceAction.Builder mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new WorkflowInstanceAction.Builder()
          .setId(rs.getLong("id"))
          .setWorkflowInstanceId(rs.getLong("workflow_id"))
          .setExecutorId(rs.getInt("executor_id"))
          .setType(WorkflowActionType.valueOf(rs.getString("type")))
          .setState(rs.getString("state"))
          .setStateText(rs.getString("state_text"))
          .setRetryNo(rs.getInt("retry_no"))
          .setExecutionStart(sqlVariants.getDateTime(rs, "execution_start"))
          .setExecutionEnd(sqlVariants.getDateTime(rs, "execution_end"));
    }
  }

  static class WorkflowActionStateRowMapper implements ResultSetExtractor<Map<Long, Map<String, String>>> {
    private final Map<Long, Map<String, String>> actionStates = new LinkedHashMap<>();

    @Override
    public Map<Long, Map<String, String>> extractData(ResultSet rs) throws SQLException {
      while (rs.next()) {
        long actionId = rs.getLong("action_id");
        String stateKey = rs.getString("state_key");
        String stateValue = rs.getString("state_value");
        actionStates.computeIfAbsent(actionId, k -> new LinkedHashMap<>()).put(stateKey, stateValue);
      }
      return actionStates;
    }
  }

  public Optional<Integer> getSignal(long workflowInstanceId) {
    return ofNullable(
        jdbc.queryForObject("select workflow_signal from nflow_workflow where id = ?", Integer.class, workflowInstanceId));
  }

  @Transactional
  public boolean setSignal(long workflowInstanceId, Optional<Integer> signal, String reason, WorkflowActionType actionType) {
    boolean updated = jdbc.update("update nflow_workflow set workflow_signal = ? where id = ?", signal.orElse(null),
        workflowInstanceId) > 0;
    if (updated) {
      DateTime now = DateTime.now();
      WorkflowInstanceAction action = new WorkflowInstanceAction.Builder()
          .setWorkflowInstanceId(workflowInstanceId)
          .setExecutionStart(now)
          .setExecutionEnd(now)
          .setState(getWorkflowInstanceState(workflowInstanceId))
          .setStateText(reason)
          .setType(actionType).build();
      insertWorkflowInstanceAction(action);
    }
    return updated;
  }

  public String getWorkflowInstanceType(long workflowInstanceId) {
    String type = workflowTypeByWorkflowIdCache.computeIfAbsent(workflowInstanceId,
        id -> jdbc.queryForObject("select type from nflow_workflow where id = ?", String.class, id).intern());
    if (workflowTypeByWorkflowIdCache.size() > workflowInstanceTypeCacheSize) {
      workflowTypeByWorkflowIdCache.clear();
    }
    return type;
  }

}
