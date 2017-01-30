package io.nflow.engine.internal.dao;

import static io.nflow.engine.internal.dao.DaoUtil.firstColumnLengthExtractor;
import static io.nflow.engine.internal.dao.DaoUtil.getInt;
import static io.nflow.engine.internal.dao.DaoUtil.toDateTime;
import static io.nflow.engine.internal.dao.DaoUtil.toTimestamp;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.recovery;
import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.sort;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.join;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.core.env.Environment;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.config.NFlow;
import io.nflow.engine.internal.executor.InstanceInfo;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.model.ModelObject;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

/**
 * Use setter injection because constructor injection may not work when nFlow is used in some legacy systems.
 */
@Component
@SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", justification = "common jdbctemplate practice")
@Singleton
public class WorkflowInstanceDao {

  static final Map<Integer, Map<String, String>> EMPTY_ACTION_STATE_MAP = Collections.<Integer, Map<String, String>> emptyMap();
  static final Logger logger = getLogger(WorkflowInstanceDao.class);

  JdbcTemplate jdbc;
  private NamedParameterJdbcTemplate namedJdbc;
  private TransactionTemplate transaction;
  ExecutorDao executorInfo;
  SQLVariants sqlVariants;
  private WorkflowInstanceExecutor workflowInstanceExecutor;
  WorkflowInstanceFactory workflowInstanceFactory;
  private long workflowInstanceQueryMaxResults;
  private long workflowInstanceQueryMaxResultsDefault;
  private long workflowInstanceQueryMaxActions;
  private long workflowInstanceQueryMaxActionsDefault;
  int instanceStateTextLength;
  int actionStateTextLength;

  @Inject
  public void setSqlVariants(SQLVariants sqlVariants) {
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
    workflowInstanceQueryMaxActions = env.getRequiredProperty("nflow.workflow.instance.query.max.actions", Long.class);
    workflowInstanceQueryMaxActionsDefault = env.getRequiredProperty("nflow.workflow.instance.query.max.actions.default",
        Long.class);
    // In one deployment, FirstColumnLengthExtractor returned 0 column length (H2), so allow explicit length setting.
    instanceStateTextLength = env.getProperty("nflow.workflow.instance.state.text.length", Integer.class, -1);
    actionStateTextLength = env.getProperty("nflow.workflow.action.state.text.length", Integer.class, -1);
  }

  @Inject
  public void setWorkflowInstanceExecutor(WorkflowInstanceExecutor workflowInstanceExecutor) {
    this.workflowInstanceExecutor = workflowInstanceExecutor;
  }

  @Inject
  public void setWorkflowInstanceFactory(WorkflowInstanceFactory workflowInstanceFactory) {
    this.workflowInstanceFactory = workflowInstanceFactory;
  }

  private int getInstanceStateTextLength() {
    if (instanceStateTextLength == -1) {
      instanceStateTextLength = jdbc.query("select state_text from nflow_workflow where 1 = 0", firstColumnLengthExtractor);
    }
    return instanceStateTextLength;
  }

  int getActionStateTextLength() {
    if (actionStateTextLength == -1) {
      actionStateTextLength = jdbc.query("select state_text from nflow_workflow_action where 1 = 0", firstColumnLengthExtractor);
    }
    return actionStateTextLength;
  }

  public int insertWorkflowInstance(WorkflowInstance instance) {
    int id;
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

  private int insertWorkflowInstanceWithCte(WorkflowInstance instance) {
    try {
      StringBuilder sqlb = new StringBuilder(256);
      sqlb.append("with wf as (").append(insertWorkflowInstanceSql()).append(" returning id)");
      Object[] instanceValues = new Object[] { instance.type, instance.rootWorkflowId, instance.parentWorkflowId,
          instance.parentActionId, instance.businessKey, instance.externalId, executorInfo.getExecutorGroup(),
          instance.status.name(), instance.state, abbreviate(instance.stateText, getInstanceStateTextLength()),
          toTimestamp(instance.nextActivation), instance.signal.orElse(null) };
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
    } catch (DuplicateKeyException e) {
      logger.warn("Failed to insert workflow instance", e);
      return -1;
    }
  }

  String insertWorkflowInstanceSql() {
    return "insert into nflow_workflow(type, root_workflow_id, parent_workflow_id, parent_action_id, business_key, external_id, "
        + "executor_group, status, state, state_text, next_activation, workflow_signal) values (?, ?, ?, ?, ?, ?, ?, "
        + sqlVariants.workflowStatus() + ", ?, ?, ?, ?)";
  }

  String insertWorkflowInstanceStateSql() {
    return "insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value)";
  }

  @SuppressFBWarnings(value = { "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE",
      "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "findbugs does not trust jdbctemplate, sql string is practically constant")
  private int insertWorkflowInstanceWithTransaction(final WorkflowInstance instance) {
    return transaction.execute(status -> {
      KeyHolder keyHolder = new GeneratedKeyHolder();
      try {
        jdbc.update((PreparedStatementCreator) connection -> {
          int p = 1;
          PreparedStatement ps = connection.prepareStatement(insertWorkflowInstanceSql(), new String[] { "id" });
          ps.setString(p++, instance.type);
          ps.setObject(p++, instance.rootWorkflowId);
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
          return ps;
        }, keyHolder);
      } catch (DuplicateKeyException e) {
        logger.warn("Failed to insert workflow instance", e);
        return -1;
      }
      int id = keyHolder.getKey().intValue();
      insertVariables(id, 0, instance.stateVariables);
      return id;
    });
  }

  void insertVariables(final int id, final int actionId, Map<String, String> changedStateVariables) {
    if (changedStateVariables.isEmpty()) {
      return;
    }
    if (sqlVariants.useBatchUpdate()) {
      insertVariablesWithBatchUpdate(id, actionId, changedStateVariables);
    } else {
      insertVariablesWithMultipleUpdates(id, actionId, changedStateVariables);
    }
  }

  private void insertVariablesWithMultipleUpdates(final int id, final int actionId, Map<String, String> changedStateVariables) {
    for (Entry<String, String> entry : changedStateVariables.entrySet()) {
      int updated = jdbc.update(insertWorkflowInstanceStateSql() + " values (?,?,?,?)", id, actionId, entry.getKey(),
          entry.getValue());
      if (updated != 1) {
        throw new IllegalStateException("Failed to insert state variable " + entry.getKey());
      }
    }
  }

  private void insertVariablesWithBatchUpdate(final int id, final int actionId, Map<String, String> changedStateVariables) {
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
    return jdbc.update(updateWorkflowInstanceSql(), instance.status.name(), instance.state,
        abbreviate(instance.stateText, getInstanceStateTextLength()), nextActivation, nextActivation, nextActivation,
        instance.status == executing ? executorInfo.getExecutorId() : null, instance.retries, instance.id);
  }

  private void updateWorkflowInstanceWithTransaction(final WorkflowInstance instance, final WorkflowInstanceAction action,
      final List<WorkflowInstance> childWorkflows, final List<WorkflowInstance> workflows,
      final Map<String, String> changedStateVariables) {
    transaction.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        updateWorkflowInstance(instance);
        int parentActionId = insertWorkflowInstanceAction(action);
        insertVariables(action.workflowInstanceId, parentActionId, changedStateVariables);
        for (WorkflowInstance childTemplate : childWorkflows) {
          Integer rootWorkflowId = instance.rootWorkflowId == null ? instance.id : instance.rootWorkflowId;
          WorkflowInstance childWorkflow = new WorkflowInstance.Builder(childTemplate).setRootWorkflowId(rootWorkflowId)
              .setParentWorkflowId(instance.id).setParentActionId(parentActionId).build();
          insertWorkflowInstance(childWorkflow);
        }
        for (WorkflowInstance workflow : workflows) {
          insertWorkflowInstance(workflow);
        }
      }
    });
  }

  public void recoverWorkflowInstancesFromDeadNodes() {
    WorkflowInstanceAction.Builder builder = new WorkflowInstanceAction.Builder().setExecutionStart(now()).setExecutionEnd(now())
        .setType(recovery).setStateText("Recovered");
    for (InstanceInfo instance : getRecoverableInstances()) {
      WorkflowInstanceAction action = builder.setState(instance.state).setWorkflowInstanceId(instance.id).build();
      recoverWorkflowInstance(instance.id, action);
    }
  }

  private List<InstanceInfo> getRecoverableInstances() {
    String sql = "select id, state from nflow_workflow where executor_id in (select id from nflow_executor where "
        + executorInfo.getExecutorGroupCondition() + " and id <> " + executorInfo.getExecutorId()
        + " and " + sqlVariants.dateLtEqDiff("expires", "current_timestamp") + ")";
    return jdbc.query(sql, new RowMapper<InstanceInfo>() {
      @Override
      public InstanceInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
        InstanceInfo instance = new InstanceInfo();
        instance.id = rs.getInt("id");
        instance.state = rs.getString("state");
        return instance;
      }
    });
  }

  private void recoverWorkflowInstance(final int instanceId, final WorkflowInstanceAction action) {
    transaction.execute(new TransactionCallbackWithoutResult() {
      @Override
      protected void doInTransactionWithoutResult(TransactionStatus status) {
        int executorId = executorInfo.getExecutorId();
        int updated = jdbc.update(
            "update nflow_workflow set executor_id = null, status = " + sqlVariants.workflowStatus(inProgress)
                + " where id = ? and executor_id in (select id from nflow_executor where "
                + executorInfo.getExecutorGroupCondition() + " and id <> " + executorId + " and "
                + sqlVariants.dateLtEqDiff("expires", "current_timestamp") + ")",
            instanceId);
        if (updated > 0) {
          insertWorkflowInstanceAction(action);
        }
      }
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
        instance.status == executing ? executorId : null, instance.retries, instance.id, executorId, action.type.name(),
        action.state, abbreviate(action.stateText, getActionStateTextLength()), action.retryNo,
        toTimestamp(action.executionStart),
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
      args.add(sqlVariants.toTimestampObject(instance.nextActivation));
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
  public boolean wakeUpWorkflowExternally(int workflowInstanceId, List<String> expectedStates) {
    StringBuilder sql = new StringBuilder("update nflow_workflow set next_activation = (case when executor_id is null then ")
        .append("case when " + sqlVariants.dateLtEqDiff("next_activation", "current_timestamp") + " then next_activation else current_timestamp end else next_activation end), ")
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

  public WorkflowInstance getWorkflowInstance(int id, Set<WorkflowInstanceInclude> includes, Long maxActions) {
    String sql = "select * from nflow_workflow w where w.id = ?";
    WorkflowInstance.Builder builder = jdbc.queryForObject(sql, new WorkflowInstanceRowMapper(), id);
    if (includes.contains(WorkflowInstanceInclude.STARTED)) {
      builder.setStarted(toDateTime(jdbc
          .queryForObject("select min(execution_start) from nflow_workflow_action where workflow_id = ?", Timestamp.class, id)));
    }
    WorkflowInstance instance = builder.build();
    if (includes.contains(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES)) {
      fillState(instance);
    }
    if (includes.contains(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS)) {
      fillChildWorkflowIds(instance);
    }
    if (includes.contains(WorkflowInstanceInclude.ACTIONS)) {
      fillActions(instance, includes.contains(WorkflowInstanceInclude.ACTION_STATE_VARIABLES), maxActions);
    }
    return instance;
  }

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
        + sqlVariants.workflowStatus(inProgress) + ") and "
        + sqlVariants.dateLtEqDiff("next_activation", "current_timestamp") + " and "
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
            return new OptimisticLockKey(rs.getInt("id"), sqlVariants.getTimestamp(rs, "modified"));
          }
        });
        if (instances.isEmpty()) {
          return emptyList();
        }
        sort(instances);
        List<Integer> ids = new ArrayList<>(instances.size());
        if (sqlVariants.useBatchUpdate()) {
          updateNextWorkflowInstancesWithBatchUpdate(instances, ids);
        } else {
          updateNextWorkflowInstancesWithMultipleUpdates(instances, ids);
        }
        return ids;
      }

      private void updateNextWorkflowInstancesWithMultipleUpdates(List<OptimisticLockKey> instances, List<Integer> ids) {
        boolean raceConditionDetected = false;
        for (OptimisticLockKey instance : instances) {
          int updated = jdbc.update(updateInstanceForExecutionQuery() + " where id = ? and modified = ? and executor_id is null",
              instance.id, instance.modified);
          if (updated == 1) {
            ids.add(instance.id);
          } else {
            raceConditionDetected = true;
          }
        }
        if (raceConditionDetected && ids.isEmpty()) {
          throw new PollingRaceConditionException("Race condition in polling workflow instances detected. "
              + "Multiple pollers using same name (" + executorInfo.getExecutorGroup() + ")");
        }
      }

      private void updateNextWorkflowInstancesWithBatchUpdate(List<OptimisticLockKey> instances, List<Integer> ids) {
        List<Object[]> batchArgs = new ArrayList<>(instances.size());
        for (OptimisticLockKey instance : instances) {
          batchArgs.add(new Object[] { instance.id, instance.modified });
          ids.add(instance.id);
        }
        int[] updateStatuses = jdbc
            .batchUpdate(updateInstanceForExecutionQuery() + " where id = ? and modified = ? and executor_id is null", batchArgs);
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
      }
    });
  }

  private static class OptimisticLockKey extends ModelObject implements Comparable<OptimisticLockKey> {
    public final int id;
    public final Object modified;

    public OptimisticLockKey(int id, Object modified) {
      this.id = id;
      this.modified = modified;
    }

    @Override
    @SuppressFBWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS", justification = "This class has a natural ordering that is inconsistent with equals")
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
    conditions.add("w.executor_group = :executor_group");
    params.addValue("executor_group", executorInfo.getExecutorGroup());
    sql += " where " + collectionToDelimitedString(conditions, " and ") + " order by w.created desc";
    sql = sqlVariants.limit(sql, ":limit");
    params.addValue("limit", getMaxResults(query.maxResults));
    List<WorkflowInstance> ret = namedJdbc.query(sql, params, new WorkflowInstanceRowMapper()).stream()
        .map(WorkflowInstance.Builder::build).collect(toList());
    for (WorkflowInstance instance : ret) {
      fillState(instance);
    }
    if (query.includeActions) {
      for (WorkflowInstance instance : ret) {
        fillActions(instance, query.includeActionStateVariables, query.maxActions);
      }
    }
    if (query.includeChildWorkflows) {
      for (final WorkflowInstance instance : ret) {
        fillChildWorkflowIds(instance);
      }
    }
    return ret;
  }

  private void fillChildWorkflowIds(final WorkflowInstance instance) {
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
    return min(maxResults.longValue(), workflowInstanceQueryMaxResults);
  }

  private void fillActions(WorkflowInstance instance, boolean includeStateVariables, Long maxActions) {
    Map<Integer, Map<String, String>> actionStates = includeStateVariables ? fetchActionStateVariables(instance)
        : EMPTY_ACTION_STATE_MAP;
    String limit = Long.toString(getMaxActions(maxActions));
    String sql = sqlVariants.limit("select * from nflow_workflow_action where workflow_id = ? order by id desc", limit);
    instance.actions.addAll(jdbc.query(sql, new WorkflowInstanceActionRowMapper(sqlVariants, actionStates), instance.id));
  }

  private long getMaxActions(Long maxActions) {
    if (maxActions == null) {
      return workflowInstanceQueryMaxActionsDefault;
    }
    return min(maxActions.longValue(), workflowInstanceQueryMaxActions);
  }

  private Map<Integer, Map<String, String>> fetchActionStateVariables(WorkflowInstance instance) {
    return jdbc.query("select * from nflow_workflow_state where workflow_id = ? order by action_id, state_key asc",
        new WorkflowActionStateRowMapper(), instance.id);
  }

  @Transactional(propagation = MANDATORY)
  public int insertWorkflowInstanceAction(final WorkflowInstance instance, final WorkflowInstanceAction action) {
    int actionId = insertWorkflowInstanceAction(action);
    insertVariables(action.workflowInstanceId, actionId, instance.getChangedStateVariables());
    return actionId;
  }

  public int insertWorkflowInstanceAction(final WorkflowInstanceAction action) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbc.update(new PreparedStatementCreator() {
      @Override
      @SuppressFBWarnings(value = { "OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE",
          "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" }, justification = "findbugs does not trust jdbctemplate, sql string is practically constant")
      public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement p = con.prepareStatement(
            insertWorkflowActionSql() + " values (?, ?, " + sqlVariants.actionType() + ", ?, ?, ?, ?, ?)", new String[] { "id" });
        int field = 1;
        p.setInt(field++, action.workflowInstanceId);
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
    return keyHolder.getKey().intValue();
  }

  public String getWorkflowInstanceState(int workflowInstanceId) {
    return jdbc.queryForObject("select state from nflow_workflow where id = ?", String.class, workflowInstanceId);
  }

  class WorkflowInstanceRowMapper implements RowMapper<WorkflowInstance.Builder> {
    @Override
    public WorkflowInstance.Builder mapRow(ResultSet rs, int rowNum) throws SQLException {
      return workflowInstanceFactory.newWorkflowInstanceBuilder() //
          .setId(rs.getInt("id")) //
          .setExecutorId(getInt(rs, "executor_id")) //
          .setRootWorkflowId(getInt(rs, "root_workflow_id")) //
          .setParentWorkflowId(getInt(rs, "parent_workflow_id")) //
          .setParentActionId(getInt(rs, "parent_action_id")) //
          .setStatus(WorkflowInstanceStatus.valueOf(rs.getString("status"))) //
          .setType(rs.getString("type")) //
          .setBusinessKey(rs.getString("business_key")) //
          .setExternalId(rs.getString("external_id")) //
          .setState(rs.getString("state")) //
          .setStateText(rs.getString("state_text")) //
          .setActions(new ArrayList<WorkflowInstanceAction>()) //
          .setNextActivation(sqlVariants.getDateTime(rs,"next_activation")) //
          .setRetries(rs.getInt("retries")) //
          .setCreated(sqlVariants.getDateTime(rs,"created")) //
          .setModified(sqlVariants.getDateTime(rs,"modified")) //
          .setExecutorGroup(rs.getString("executor_group")) //
          .setSignal(ofNullable(getInt(rs, "workflow_signal")));
    }
  }

  static class WorkflowInstanceActionRowMapper implements RowMapper<WorkflowInstanceAction> {
    private final SQLVariants sqlVariants;
    private final Map<Integer, Map<String, String>> actionStates;

    public WorkflowInstanceActionRowMapper(SQLVariants sqlVariants, Map<Integer, Map<String, String>> actionStates) {
      this.sqlVariants = sqlVariants;
      this.actionStates = actionStates;
    }

    @Override
    public WorkflowInstanceAction mapRow(ResultSet rs, int rowNum) throws SQLException {
      int actionId = rs.getInt("id");
      Map<String, String> actionState = actionStates.getOrDefault(actionId, emptyMap());
      return new WorkflowInstanceAction.Builder() //
          .setId(actionId) //
          .setWorkflowInstanceId(rs.getInt("workflow_id")) //
          .setExecutorId(rs.getInt("executor_id")) //
          .setType(WorkflowActionType.valueOf(rs.getString("type"))) //
          .setState(rs.getString("state")) //
          .setStateText(rs.getString("state_text")) //
          .setUpdatedStateVariables(actionState) //
          .setRetryNo(rs.getInt("retry_no")) //
          .setExecutionStart(sqlVariants.getDateTime(rs, "execution_start")) //
          .setExecutionEnd(sqlVariants.getDateTime(rs, "execution_end")).build();
    }
  }

  static class WorkflowActionStateRowMapper implements ResultSetExtractor<Map<Integer, Map<String, String>>> {
    private final Map<Integer, Map<String, String>> actionStates = new LinkedHashMap<>();

    @Override
    public Map<Integer, Map<String, String>> extractData(ResultSet rs) throws SQLException {
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

  public Optional<Integer> getSignal(Integer workflowInstanceId) {
    return ofNullable(
        jdbc.queryForObject("select workflow_signal from nflow_workflow where id = ?", Integer.class, workflowInstanceId));
  }

  @Transactional
  public boolean setSignal(Integer workflowInstanceId, Optional<Integer> signal, String reason, WorkflowActionType actionType) {
    boolean updated = jdbc.update("update nflow_workflow set workflow_signal = ? where id = ?", signal.orElse(null),
        workflowInstanceId) > 0;
    if (updated) {
      DateTime now = DateTime.now();
      WorkflowInstanceAction action = new WorkflowInstanceAction.Builder() //
          .setWorkflowInstanceId(workflowInstanceId) //
          .setExecutionStart(now) //
          .setExecutionEnd(now) //
          .setState(getWorkflowInstanceState(workflowInstanceId)) //
          .setStateText(reason) //
          .setType(actionType).build();
      insertWorkflowInstanceAction(action);
    }
    return updated;
  }

  public String getWorkflowInstanceType(Integer workflowInstanceId) {
    return jdbc.queryForObject("select type from nflow_workflow where id = ?", String.class, workflowInstanceId);
  }

}
