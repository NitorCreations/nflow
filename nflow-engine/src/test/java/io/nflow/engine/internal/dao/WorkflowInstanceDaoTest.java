package io.nflow.engine.internal.dao;

import static io.nflow.engine.service.WorkflowInstanceInclude.CHILD_WORKFLOW_IDS;
import static io.nflow.engine.service.WorkflowInstanceInclude.CURRENT_STATE_VARIABLES;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static io.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.manual;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.recovery;
import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import io.nflow.engine.config.db.PgDatabaseConfiguration.PostgreSQLVariants;
import io.nflow.engine.internal.dao.WorkflowInstanceDao.WorkflowInstanceActionRowMapper;
import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;
import io.nflow.engine.internal.storage.db.SQLVariants;
import io.nflow.engine.service.WorkflowInstanceInclude;
import io.nflow.engine.workflow.executor.StateVariableValueTooLongException;
import io.nflow.engine.workflow.instance.QueryWorkflowInstances;
import io.nflow.engine.workflow.instance.WorkflowInstance;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction;
import io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType;
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory;

public class WorkflowInstanceDaoTest extends BaseDaoTest {

  @Inject
  WorkflowInstanceDao dao;
  @Inject
  ExecutorDao executorDao;
  @Inject
  TransactionTemplate transaction;
  @Inject
  WorkflowInstanceExecutor workflowInstanceExecutor;
  @Inject
  WorkflowInstanceFactory workflowInstanceFactory;
  @Inject
  SQLVariants sqlVariant;
  @Inject
  Environment env;
  List<WorkflowInstance> noChildWorkflows = emptyList();
  List<WorkflowInstance> emptyWorkflows = emptyList();

  @Test
  public void roundTripTest() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("a", "1");
    long id = dao.insertWorkflowInstance(i1);
    WorkflowInstance i2 = dao.getWorkflowInstance(id, EnumSet.allOf(WorkflowInstanceInclude.class), null);
    assertThat(i2.id, notNullValue());
    assertThat(i2.created, notNullValue());
    assertThat(i2.modified, notNullValue());
    checkSameWorkflowInfo(i1, i2);
  }

  @Test
  public void queryNonExistingWorkflowThrowsException() {
    assertThrows(EmptyResultDataAccessException.class, () -> dao.getWorkflowInstance(-42, emptySet(), null));
  }

  @Test
  public void queryWorkflowInstanceWithAllConditions() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("b", "2");
    long workflowId = dao.insertWorkflowInstance(i1);
    assertThat(workflowId, not(equalTo(-1)));

    WorkflowInstanceAction action = constructActionBuilder(workflowId).build();
    long actionId = dao.insertWorkflowInstanceAction(action);

    WorkflowInstance child = constructWorkflowInstanceBuilder().setParentWorkflowId(workflowId).setParentActionId(actionId)
        .build();
    long childId = dao.insertWorkflowInstance(child);
    assertThat(childId, not(equalTo(-1)));
    dao.insertWorkflowInstanceAction(constructActionBuilder(childId).build());
    dao.insertWorkflowInstanceAction(constructActionBuilder(childId).build());

    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder() //
        .addIds(childId) //
        .addTypes(child.type) //
        .addStates(child.state) //
        .addStatuses(i1.status) //
        .setParentWorkflowId(workflowId) //
        .setParentActionId(actionId) //
        .setBusinessKey(child.businessKey) //
        .setExternalId(child.externalId) //
        .setIncludeActions(true) //
        .setIncludeActionStateVariables(true) //
        .setIncludeCurrentStateVariables(true) //
        .setIncludeChildWorkflows(true) //
        .setMaxResults(1L) //
        .setMaxActions(1L).build();
    List<WorkflowInstance> l = dao.queryWorkflowInstances(q);
    assertThat(l.size(), is(1));
    checkSameWorkflowInfo(child, l.get(0));
    assertThat(l.get(0).actions.size(), is(1));
  }

  @Test
  public void queryWorkflowInstanceWithMinimalConditions() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    long id = dao.insertWorkflowInstance(i1);
    assertThat(id, not(equalTo(-1)));
    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().build();
    List<WorkflowInstance> createdInstances = dao.queryWorkflowInstances(q);
    assertThat(createdInstances.size(), is(1));
    WorkflowInstance instance = createdInstances.get(0);
    WorkflowInstance originalWithoutStateVariables = new WorkflowInstance.Builder(i1).setStateVariables(emptyMap()).build();
    checkSameWorkflowInfo(originalWithoutStateVariables, instance);
    assertNull(instance.started);
  }

  @Test
  public void updateWorkflowInstance() throws InterruptedException {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setStatus(created).build();
    long id = dao.insertWorkflowInstance(i1);
    List<Long> ids = dao.pollNextWorkflowInstanceIds(1);
    assertThat(ids, contains(id));
    DateTime started = now();
    final WorkflowInstance i2 = new WorkflowInstance.Builder(
        dao.getWorkflowInstance(id, EnumSet.of(CURRENT_STATE_VARIABLES), null)).setStatus(inProgress).setState("updateState")
            .setStateText("update text").setStartedIfNotSet(started).setBusinessKey("newBusinessKey").build();
    final WorkflowInstance polledInstance = dao.getWorkflowInstance(id, emptySet(), null);
    assertThat(polledInstance.status, equalTo(executing));
    final DateTime originalModifiedTime = polledInstance.modified;
    sleep(1);
    WorkflowInstanceAction action = constructActionBuilder(id).build();
    WorkflowInstance newWorkflow = constructWorkflowInstanceBuilder().setBusinessKey("newKey").build();

    dao.updateWorkflowInstanceAfterExecution(i2, action, noChildWorkflows, asList(newWorkflow), false);

    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("status"), equalTo(inProgress.name()));
        assertThat(rs.getString("state"), equalTo(i2.state));
        assertThat(rs.getString("state_text"), equalTo(i2.stateText));
        assertThat(rs.getTimestamp("next_activation").getTime(), equalTo(i2.nextActivation.toDate().getTime()));
        assertThat(rs.getInt("executor_id") != 0, equalTo(i2.status == executing));
        assertThat(rs.getTimestamp("modified").getTime(), greaterThan(originalModifiedTime.getMillis()));
        assertThat(rs.getTimestamp("started").getTime(), is(equalTo(started.getMillis())));
        assertThat(rs.getString("business_key"), is(equalTo(i2.businessKey)));
      }
    });
    QueryWorkflowInstances query = new QueryWorkflowInstances.Builder().setBusinessKey("newKey").build();
    List<WorkflowInstance> instances = dao.queryWorkflowInstances(query);
    assertThat(instances.size(), is(1));
    for (WorkflowInstance instance : instances) {
      assertThat(instance.parentWorkflowId, is(nullValue()));
      assertThat(instance.businessKey, is("newKey"));
    }
  }

  @Test
  public void updateWorkflowInstanceDoesNotCreateActionWhenCreateActionIsFalse() {
    WorkflowInstance instance = updateInstanceBuilder().build();
    WorkflowInstanceAction action = constructActionBuilder(instance.id).build();

    dao.updateWorkflowInstanceAfterExecution(instance, action, noChildWorkflows, emptyWorkflows, false);

    assertThat(updatedInstance().actions.isEmpty(), is(true));
  }

  @Test
  public void updateWorkflowInstanceCreatesActionWhenCreateActionIsTrue() {
    WorkflowInstance instance = updateInstanceBuilder().build();
    WorkflowInstanceAction action = constructActionBuilder(instance.id).build();

    dao.updateWorkflowInstanceAfterExecution(instance, action, noChildWorkflows, emptyWorkflows, true);

    assertThat(updatedInstance().actions.size(), is(1));
  }

  @Test
  public void updateWorkflowInstanceCreatesActionWhenChildWorkflowIsCreated() {
    WorkflowInstance instance = updateInstanceBuilder().build();
    WorkflowInstanceAction action = constructActionBuilder(instance.id).build();
    WorkflowInstance childWorkflow = constructWorkflowInstanceBuilder().build();

    dao.updateWorkflowInstanceAfterExecution(instance, action, asList(childWorkflow), emptyWorkflows, false);

    assertThat(updatedInstance().actions.size(), is(1));
  }

  @Test
  public void updateWorkflowInstanceCreatesActionWhenNewWorkflowIsCreated() {
    WorkflowInstance instance = updateInstanceBuilder().build();
    WorkflowInstanceAction action = constructActionBuilder(instance.id).build();
    WorkflowInstance newWorkflow = constructWorkflowInstanceBuilder().build();

    dao.updateWorkflowInstanceAfterExecution(instance, action, noChildWorkflows, asList(newWorkflow), false);

    assertThat(updatedInstance().actions.size(), is(1));
  }

  @Test
  public void updateWorkflowInstanceCreatesActionWhenStateVariablesAreModified() {
    WorkflowInstance instance = updateInstanceBuilder().putStateVariable("foo", "bar").build();
    WorkflowInstanceAction action = constructActionBuilder(instance.id).build();

    dao.updateWorkflowInstanceAfterExecution(instance, action, noChildWorkflows, emptyWorkflows, false);

    assertThat(updatedInstance().actions.size(), is(1));
  }

  private WorkflowInstance.Builder updateInstanceBuilder() {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setBusinessKey("updatedKey").build();
    long id = dao.insertWorkflowInstance(instance);
    return new WorkflowInstance.Builder(
        dao.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null)) //
            .setStatus(inProgress) //
            .setState("updateState") //
            .setStateText("update text");
  }

  private WorkflowInstance updatedInstance() {
    QueryWorkflowInstances query = new QueryWorkflowInstances.Builder() //
        .setBusinessKey("updatedKey") //
        .setIncludeActions(true).build();
    List<WorkflowInstance> instances = dao.queryWorkflowInstances(query);
    assertThat(instances.size(), is(1));
    return instances.get(0);
  }

  @Test
  public void updateWorkflowInstanceWithChildWorkflowHavingExistinExternalIdWorks() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setStatus(created).build();
    long id = dao.insertWorkflowInstance(i1);
    WorkflowInstance i2 = new WorkflowInstance.Builder(
        dao.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null)) //
            .setStatus(inProgress) //
            .setState("updateState") //
            .setStateText("update text").build();
    DateTime started = now();
    WorkflowInstanceAction a1 = new WorkflowInstanceAction.Builder() //
        .setExecutionStart(started) //
        .setExecutorId(42) //
        .setExecutionEnd(started.plusMillis(100)) //
        .setRetryNo(1) //
        .setState("test") //
        .setStateText("state text") //
        .setWorkflowInstanceId(id) //
        .setType(stateExecution).build();
    WorkflowInstance childWorkflow1 = constructWorkflowInstanceBuilder() //
        .setBusinessKey("childKey") //
        .setExternalId("extId") //
        .putStateVariable("key", "value").build();
    WorkflowInstance childWorkflow2 = constructWorkflowInstanceBuilder() //
        .setBusinessKey("childKey") //
        .setExternalId("extId") //
        .putStateVariable("key", "newValue") //
        .putStateVariable("newKey", "value").build();

    dao.updateWorkflowInstanceAfterExecution(i2, a1, asList(childWorkflow1, childWorkflow2), emptyWorkflows, false);

    Map<Long, List<Long>> childWorkflows = dao.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS),
        null).childWorkflows;
    assertThat(childWorkflows.size(), is(1));
    for (List<Long> childIds : childWorkflows.values()) {
      assertThat(childIds.size(), is(1));
      WorkflowInstance childInstance = dao.getWorkflowInstance(childIds.get(0),
          EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null);
      assertThat(childInstance.parentWorkflowId, is(id));
      assertThat(childInstance.businessKey, is("childKey"));
      assertThat(childInstance.externalId, is("extId"));
      assertThat(childInstance.stateVariables.get("key"), is("value"));
      assertThat(childInstance.stateVariables.get("newKey"), is(nullValue()));
    }
  }

  @Test
  public void updateWorkflowInstanceWithChildWorkflowsWorks() {
    // create 3 level hierarchy of workflows
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setStatus(created).build();
    long id = dao.insertWorkflowInstance(i1);
    WorkflowInstance i2 = new WorkflowInstance.Builder(
        dao.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null)).setStatus(inProgress)
            .setState("updateState").setStateText("update text").build();
    DateTime started = now();
    WorkflowInstanceAction a1 = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
        .setExecutionEnd(started.plusMillis(100)).setRetryNo(1).setState("test").setStateText("state text")
        .setWorkflowInstanceId(id).setType(stateExecution).build();

    WorkflowInstance middleWorkflow = constructWorkflowInstanceBuilder().setBusinessKey("middleKey").build();

    dao.updateWorkflowInstanceAfterExecution(i2, a1, asList(middleWorkflow), emptyWorkflows, false);

    long middleWorkflowId = -1;
    for (List<Long> childIds : dao.getWorkflowInstance(id, EnumSet.of(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS),
        null).childWorkflows.values()) {
      middleWorkflowId = childIds.get(0);
    }

    middleWorkflow = new WorkflowInstance.Builder(
        dao.getWorkflowInstance(middleWorkflowId, EnumSet.of(WorkflowInstanceInclude.CURRENT_STATE_VARIABLES), null))
            .setStatus(inProgress).setState("updateState").setStateText("update text").build();

    WorkflowInstanceAction middleAction = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
        .setExecutionEnd(started.plusMillis(100)).setRetryNo(1).setState("test").setStateText("state text")
        .setWorkflowInstanceId(middleWorkflow.id).setType(stateExecution).build();

    WorkflowInstance childWorkflow = constructWorkflowInstanceBuilder().setBusinessKey("childKey").build();
    dao.updateWorkflowInstanceAfterExecution(middleWorkflow, middleAction, asList(childWorkflow), emptyWorkflows, false);

    Map<Long, List<Long>> childWorkflows = dao.getWorkflowInstance(middleWorkflowId,
        EnumSet.of(WorkflowInstanceInclude.CHILD_WORKFLOW_IDS), null).childWorkflows;
    assertThat(childWorkflows.size(), is(1));
    for (List<Long> childIds : childWorkflows.values()) {
      assertThat(childIds.size(), is(1));
      WorkflowInstance childInstance = dao.getWorkflowInstance(childIds.get(0), emptySet(), null);
      assertThat(childInstance.parentWorkflowId, is(middleWorkflowId));
      assertThat(childInstance.businessKey, is("childKey"));
    }
  }

  @Test
  public void updateNotRunningWorkflowInstanceUpdatesStatusForNotRunningInstance() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    long id = dao.insertWorkflowInstance(instance);
    WorkflowInstance modifiedInstance = new WorkflowInstance.Builder(instance).setId(id).setState(null).setNextActivation(null)
        .setStatus(manual).setStateText("modified").build();
    boolean updated = dao.updateNotRunningWorkflowInstance(modifiedInstance);
    assertThat(updated, is(true));
    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("status"), is(manual.name()));
        assertThat(rs.getString("state"), is(instance.state));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(instance.nextActivation.getMillis()));
        assertThat(rs.getString("state_text"), is("modified"));
        assertThat(rs.getString("business_key"), is(instance.businessKey));
      }
    });
  }

  @Test
  public void updateNotRunningWorkflowInstanceUpdatesStateForNotRunningInstance() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    long id = dao.insertWorkflowInstance(instance);
    WorkflowInstance modifiedInstance = new WorkflowInstance.Builder(instance).setId(id).setState("manualState")
        .setNextActivation(null).setStatus(null).setStateText("modified").build();
    boolean updated = dao.updateNotRunningWorkflowInstance(modifiedInstance);
    assertThat(updated, is(true));
    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("status"), is(instance.status.name()));
        assertThat(rs.getString("state"), is("manualState"));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(instance.nextActivation.getMillis()));
        assertThat(rs.getString("state_text"), is("modified"));
        assertThat(rs.getString("business_key"), is(instance.businessKey));
      }
    });
  }

  @Test
  public void updateNotRunningWorkflowInstanceUpdatesNextActivationForNotRunningInstance() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    long id = dao.insertWorkflowInstance(instance);
    final DateTime tomorrow = now().plusDays(1);
    WorkflowInstance modifiedInstance = new WorkflowInstance.Builder(instance).setId(id).setState(null)
        .setNextActivation(tomorrow).setStatus(null).setStateText("modified").build();
    boolean updated = dao.updateNotRunningWorkflowInstance(modifiedInstance);
    assertThat(updated, is(true));
    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("status"), is(instance.status.name()));
        assertThat(rs.getString("state"), is(instance.state));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(tomorrow.getMillis()));
        assertThat(rs.getString("state_text"), is("modified"));
        assertThat(rs.getString("business_key"), is(instance.businessKey));
      }
    });
  }

  @Test
  public void updateNotRunningWorkflowInstanceUpdatesBusinessKeyForNotRunningInstance() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    long id = dao.insertWorkflowInstance(instance);
    WorkflowInstance modifiedInstance = new WorkflowInstance.Builder(instance).setId(id).setBusinessKey("modifiedKey")
        .setStateText("modified").build();
    boolean updated = dao.updateNotRunningWorkflowInstance(modifiedInstance);
    assertThat(updated, is(true));
    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("status"), is(instance.status.name()));
        assertThat(rs.getString("state"), is(instance.state));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(instance.nextActivation.getMillis()));
        assertThat(rs.getString("state_text"), is("modified"));
        assertThat(rs.getString("business_key"), is("modifiedKey"));
      }
    });
  }

  @Test
  public void updateNotRunningWorkflowInstanceDoesNotUpdateRunningInstance() {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    long id = dao.insertWorkflowInstance(instance);
    assertThat(jdbc.update("update nflow_workflow set executor_id = ? where id = ?", executorDao.getExecutorId(), id), is(1));
    final DateTime tomorrow = now().plusDays(1);
    WorkflowInstance modifiedInstance = new WorkflowInstance.Builder(instance).setId(id).setState("manualState")
        .setNextActivation(tomorrow).setStatus(manual).build();
    boolean updated = dao.updateNotRunningWorkflowInstance(modifiedInstance);
    assertThat(updated, is(false));
  }

  @Test
  public void updatingNextActivationToNullWhileExternalNextActivationIsNotNull() {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    long workflowId = dao.insertWorkflowInstance(instance);
    assertTrue(workflowId > -1);
    jdbc.update("update nflow_workflow set executor_id = ? where id = ?", executorDao.getExecutorId(), workflowId);
    assertThat(jdbc.update("update nflow_workflow set external_next_activation = ? where id = ?",
        new Timestamp(now().getMillis()), workflowId), is(1));
    WorkflowInstance i = dao.getWorkflowInstance(workflowId, EnumSet.of(CURRENT_STATE_VARIABLES), null);
    i = new WorkflowInstance.Builder(i).setNextActivation(null).build();

    dao.updateWorkflowInstance(i);

    WorkflowInstance updated = dao.getWorkflowInstance(workflowId, emptySet(), null);
    assertThat(updated.nextActivation, is(nullValue()));
  }

  @Test
  public void updatingNextActivationWhenExternalNextActivationIsEarlier() {
    DateTime now = now();
    DateTime future = now.plusDays(1);

    WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(future).build();

    final long workflowId = dao.insertWorkflowInstance(instance);
    jdbc.update("update nflow_workflow set executor_id = ? where id = ?", executorDao.getExecutorId(), workflowId);

    assertTrue(workflowId > -1);
    assertThat(jdbc.update("update nflow_workflow set external_next_activation = ? where id = ?", new Timestamp(now.getMillis()),
        workflowId), is(1));

    WorkflowInstance i = dao.getWorkflowInstance(workflowId, EnumSet.of(CURRENT_STATE_VARIABLES), null);

    assertThat(dao.updateWorkflowInstance(i), is(1));
    assertThat(dao.getWorkflowInstance(workflowId, emptySet(), null).nextActivation, is(now));
  }

  @Test
  public void updatingNextActivationWhenExternalNextActivationIsLater() {
    DateTime now = now();
    DateTime future = now.plusDays(1);

    WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(now).build();
    long workflowId = dao.insertWorkflowInstance(instance);
    jdbc.update("update nflow_workflow set executor_id = ? where id = ?", executorDao.getExecutorId(), workflowId);

    assertTrue(workflowId > -1);
    assertThat(jdbc.update("update nflow_workflow set external_next_activation = ? where id = ?",
        new Timestamp(future.getMillis()), workflowId), is(1));
    WorkflowInstance i = dao.getWorkflowInstance(workflowId, EnumSet.of(CURRENT_STATE_VARIABLES), null);
    assertThat(dao.updateWorkflowInstance(i), is(1));
    assertThat(dao.getWorkflowInstance(workflowId, emptySet(), null).nextActivation, is(now));
  }

  @Test
  public void postgreSQLUpdateWithoutActionIsNotAllowed() throws InterruptedException {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setStatus(created).build();
    long id = dao.insertWorkflowInstance(i1);
    List<Long> ids = dao.pollNextWorkflowInstanceIds(1);
    assertThat(ids, contains(id));
    final WorkflowInstance i2 = new WorkflowInstance.Builder(
        dao.getWorkflowInstance(id, EnumSet.of(CURRENT_STATE_VARIABLES), null)).setStatus(inProgress).setState("updateState")
            .setStateText("update text").build();
    sleep(1);
    assertThrows(IllegalArgumentException.class,
        () -> dao.updateWorkflowInstanceAfterExecution(i2, null, noChildWorkflows, emptyWorkflows, false));
  }

  @Test
  public void fakePostgreSQLupdateWorkflowInstance() {
    JdbcTemplate j = mock(JdbcTemplate.class);
    WorkflowInstanceDao d = preparePostgreSQLDao(j);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> args = ArgumentCaptor.forClass(Object.class);
    when(j.queryForObject(sql.capture(), eq(Long.class), args.capture())).thenReturn(42L);

    DateTime started = now();
    WorkflowInstance i2 = new WorkflowInstance.Builder().setState("updateState").setStateText("update text")
        .setNextActivation(started.plusSeconds(1)).setStatus(executing).setRetries(3).setId(43).putStateVariable("A", "B")
        .build();
    WorkflowInstanceAction a1 = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(4)
        .setExecutionEnd(started.plusMillis(100)).setRetryNo(1).setType(externalChange).setState("test")
        .setStateText("state text").setWorkflowInstanceId(43).build();

    d.updateWorkflowInstanceAfterExecution(i2, a1, noChildWorkflows, emptyWorkflows, false);
    assertEquals("with wf as (update nflow_workflow set status = ?::workflow_status, state = ?, state_text = ?, "
        + "next_activation = (case when ?::timestamptz is null then null when external_next_activation is null then "
        + "?::timestamptz else least(?::timestamptz, external_next_activation) end), external_next_activation = null, "
        + "executor_id = ?, retries = ?, business_key = ?, started = (case when started is null then ? else started end) "
        + "where id = ? and executor_id = 42 returning id), "
        + "act as (insert into nflow_workflow_action(workflow_id, executor_id, type, state, state_text, retry_no, "
        + "execution_start, execution_end) select wf.id, ?, ?::action_type, ?, ?, ?, ?, ? from wf returning id), "
        + "ins18 as (insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value) "
        + "select wf.id,act.id,?,? from wf,act) select act.id from act", sql.getValue());
    assertThat(args.getAllValues().size(), is(countMatches(sql.getValue(), "?")));

    int i = 0;
    assertThat(args.getAllValues().get(i++), is((Object) i2.status.name()));
    assertThat(args.getAllValues().get(i++), is((Object) i2.state));
    assertThat(args.getAllValues().get(i++), is((Object) i2.stateText));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(i2.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(i2.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(i2.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) 42));
    assertThat(args.getAllValues().get(i++), is((Object) i2.retries));
    assertThat(args.getAllValues().get(i++), is((Object) i2.businessKey));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(a1.executionStart.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) i2.id));
    assertThat(args.getAllValues().get(i++), is((Object) 42));
    assertThat(args.getAllValues().get(i++), is((Object) a1.type.name()));
    assertThat(args.getAllValues().get(i++), is((Object) a1.state));
    assertThat(args.getAllValues().get(i++), is((Object) a1.stateText));
    assertThat(args.getAllValues().get(i++), is((Object) a1.retryNo));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(a1.executionStart.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(a1.executionEnd.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) "A"));
    assertThat(args.getAllValues().get(i++), is((Object) "B"));
  }

  @Test
  public void fakePostgreSQLinsertWorkflowInstance() {
    JdbcTemplate j = mock(JdbcTemplate.class);
    WorkflowInstanceDao d = preparePostgreSQLDao(j);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> args = ArgumentCaptor.forClass(Object.class);
    when(j.queryForObject(sql.capture(), eq(Long.class), args.capture())).thenReturn(42L);

    DateTime started = now();
    WorkflowInstance wf = new WorkflowInstance.Builder().setStatus(inProgress).setState("updateState").setStateText("update text")
        .setParentWorkflowId(110L).setParentActionId(421L).setNextActivation(started.plusSeconds(1)).setRetries(3).setId(43)
        .putStateVariable("A", "B").putStateVariable("C", "D").setSignal(Optional.of(1)).setStartedIfNotSet(started)
        .setPriority((short) 10).build();

    d.insertWorkflowInstance(wf);
    assertEquals("with wf as (insert into nflow_workflow(type, priority, parent_workflow_id, parent_action_id, business_key, "
        + "external_id, executor_group, status, state, state_text, next_activation, workflow_signal) values "
        + "(?, ?, ?, ?, ?, ?, ?, ?::workflow_status, ?, ?, ?, ?) returning id), ins12 as "
        + "(insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value) select wf.id,0,?,? from wf), "
        + "ins14 as (insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value) "
        + "select wf.id,0,?,? from wf) select wf.id from wf", sql.getValue());
    assertThat(args.getAllValues().size(), is(countMatches(sql.getValue(), "?")));

    int i = 0;
    assertThat(args.getAllValues().get(i++), is((Object) wf.type));
    assertThat(args.getAllValues().get(i++), is((Object) wf.priority));
    assertThat(args.getAllValues().get(i++), is((Object) wf.parentWorkflowId));
    assertThat(args.getAllValues().get(i++), is((Object) wf.parentActionId));
    assertThat(args.getAllValues().get(i++), is((Object) wf.businessKey));
    assertThat(args.getAllValues().get(i++), is((Object) wf.externalId));
    assertThat(args.getAllValues().get(i++), is((Object) wf.executorGroup));
    assertThat(args.getAllValues().get(i++), is((Object) wf.status.name()));
    assertThat(args.getAllValues().get(i++), is((Object) wf.state));
    assertThat(args.getAllValues().get(i++), is((Object) wf.stateText));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(wf.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) wf.signal.get()));
    assertThat(args.getAllValues().get(i++), is((Object) "A"));
    assertThat(args.getAllValues().get(i++), is((Object) "B"));
    assertThat(args.getAllValues().get(i++), is((Object) "C"));
    assertThat(args.getAllValues().get(i++), is((Object) "D"));
  }

  @Test
  public void insertWorkflowInstanceActionWorks() {
    DateTime started = now();
    final WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("a", "1");
    long id = dao.insertWorkflowInstance(i1);
    final WorkflowInstanceAction a1 = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
        .setExecutionEnd(started.plusMillis(100)).setRetryNo(1).setType(stateExecution).setState("test")
        .setStateText("state text").setWorkflowInstanceId(id).build();
    i1.stateVariables.put("b", "2");
    transaction.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(TransactionStatus status) {
        dao.insertWorkflowInstanceAction(i1, a1);
        return null;
      }
    });
    WorkflowInstance createdInstance = dao.getWorkflowInstance(id, EnumSet.allOf(WorkflowInstanceInclude.class), null);
    checkSameWorkflowInfo(i1, createdInstance);
  }

  @Test
  public void pollNextWorkflowInstances() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(now().minusMinutes(1)).setExecutorGroup("junit")
        .build();
    long id = dao.insertWorkflowInstance(i1);
    List<Long> firstBatch = dao.pollNextWorkflowInstanceIds(100);
    List<Long> secondBatch = dao.pollNextWorkflowInstanceIds(100);
    assertThat(firstBatch.size(), equalTo(1));
    assertThat(firstBatch.get(0), equalTo(id));
    assertThat(secondBatch.size(), equalTo(0));
  }

  @Test
  public void pollNextWorkflowInstancesReturnInstancesInCorrectOrder() {
    long olderLowPrio = createInstance(2, (short) 1);
    long newerLowPrio = createInstance(1, (short) 1);
    long newerHighPrio = createInstance(1, (short) 2);

    // high priority comes first
    List<Long> ids = dao.pollNextWorkflowInstanceIds(1);
    assertThat(ids, is(asList(newerHighPrio)));

    // older comes first when same prio
    ids = dao.pollNextWorkflowInstanceIds(1);
    assertThat(ids, is(asList(olderLowPrio)));

    // newer comes last when same prio
    ids = dao.pollNextWorkflowInstanceIds(1);
    assertThat(ids, is(asList(newerLowPrio)));
  }

  private long createInstance(int minutesInPast, short priority) {
    return dao.insertWorkflowInstance(constructWorkflowInstanceBuilder().setNextActivation(now().minusMinutes(minutesInPast))
        .setPriority(priority).setExecutorGroup("junit").build());
  }

  @Test
  public void fakePostgreSQLpollNextWorkflowInstances() {
    JdbcTemplate j = mock(JdbcTemplate.class);
    WorkflowInstanceDao d = preparePostgreSQLDao(j);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    when(j.queryForList(sql.capture(), eq(Long.class))).thenReturn(asList(1L, 2L, 3L));
    assertThat(d.pollNextWorkflowInstanceIds(5), is(asList(1L, 2L, 3L)));
    assertEquals(
        "update nflow_workflow set executor_id = 42, status = 'executing'::workflow_status, external_next_activation = null where id in (select id from nflow_workflow where executor_id is null and status in ('created'::workflow_status, 'inProgress'::workflow_status) and next_activation <= current_timestamp and group matches order by priority desc, next_activation asc limit 5 for update skip locked) and executor_id is null returning id",
        sql.getValue());
  }

  private WorkflowInstanceDao preparePostgreSQLDao(JdbcTemplate jdbcTemplate) {
    ExecutorDao eDao = mock(ExecutorDao.class);
    lenient().when(eDao.getExecutorGroupCondition()).thenReturn("group matches");
    lenient().when(eDao.getExecutorId()).thenReturn(42);
    NamedParameterJdbcTemplate namedJdbc = mock(NamedParameterJdbcTemplate.class);
    TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    WorkflowInstanceDao d = new WorkflowInstanceDao(new PostgreSQLVariants(), jdbcTemplate, transactionTemplate, namedJdbc, eDao,
        workflowInstanceExecutor, workflowInstanceFactory, env);

    d.instanceStateTextLength = 128;
    d.actionStateTextLength = 128;
    d.stateVariableValueMaxLength = 128;
    return d;
  }

  @Test
  public void pollNextWorkflowInstancesWithPartialRaceCondition() throws InterruptedException {
    int batchSize = 100;
    for (int i = 0; i < batchSize; i++) {
      WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(now().minusMinutes(1))
          .setExecutorGroup("junit").build();
      dao.insertWorkflowInstance(instance);
    }
    Poller[] pollers = new Poller[] { new Poller(dao, batchSize), new Poller(dao, batchSize) };
    for (int i = 0; i < 10; ++i) {
      Thread[] threads = new Thread[] { new Thread(pollers[0]), new Thread(pollers[1]) };
      threads[0].start();
      threads[1].start();
      threads[0].join();
      threads[1].join();
      assertThat(pollers[0].returnSize + pollers[1].returnSize, is(batchSize));
      if (pollers[0].detectedRaceCondition || pollers[1].detectedRaceCondition
          || (pollers[0].returnSize < batchSize && pollers[1].returnSize < batchSize)) {
        return;
      }
    }
    fail("Race condition should happen");
  }

  @Test
  public void wakesUpSleepingWorkflow() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    long id = dao.insertWorkflowInstance(i1);
    assertThat(dao.getWorkflowInstance(id, emptySet(), null).nextActivation, nullValue());
    dao.wakeupWorkflowInstanceIfNotExecuting(id, new ArrayList<String>());
    assertThat(dao.getWorkflowInstance(id, emptySet(), null).nextActivation, notNullValue());
  }

  @Test
  public void doesNotWakeUpRunningWorkflow() {
    DateTime past = now().minusDays(1);
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setExecutorGroup("junit").setNextActivation(past).build();
    long id = dao.insertWorkflowInstance(i1);
    List<Long> ids = dao.pollNextWorkflowInstanceIds(1);
    assertThat(ids, contains(id));
    assertThat(dao.getWorkflowInstance(id, emptySet(), null).nextActivation, is(past));
    dao.wakeupWorkflowInstanceIfNotExecuting(id, asList(i1.state));
    assertThat(dao.getWorkflowInstance(id, emptySet(), null).nextActivation, is(past));
  }

  @Test
  public void wakesUpWorkflowInMatchingState() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    long id = dao.insertWorkflowInstance(i1);
    assertThat(dao.getWorkflowInstance(id, emptySet(), null).nextActivation, nullValue());
    dao.wakeupWorkflowInstanceIfNotExecuting(id, asList("otherState", i1.state));
    assertThat(dao.getWorkflowInstance(id, emptySet(), null).nextActivation, notNullValue());
  }

  @Test
  public void getWorkflowInstanceStateWorks() {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    long workflowInstanceId = dao.insertWorkflowInstance(instance);

    String state = dao.getWorkflowInstanceState(workflowInstanceId);

    assertThat(state, is("CreateLoan"));
  }

  @Test
  public void insertingSubWorkflowWorks() {
    DateTime started = now();
    final WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("b", "2");
    long parentWorkflowId = dao.insertWorkflowInstance(i1);
    assertThat(parentWorkflowId, not(equalTo(-1)));

    long parentActionId = addWorkflowAction(parentWorkflowId, i1, started, started.plusMillis(100));
    WorkflowInstance createdInstance = dao.getWorkflowInstance(parentWorkflowId, EnumSet.allOf(WorkflowInstanceInclude.class),
        null);
    checkSameWorkflowInfo(i1, createdInstance);

    long subWorkflowId1 = addSubWorkflow(parentWorkflowId, parentActionId);
    assertThat(subWorkflowId1, not(equalTo(-1)));

    long subWorkflowId2 = addSubWorkflow(parentWorkflowId, parentActionId);
    assertThat(subWorkflowId2, not(equalTo(-1)));

    WorkflowInstance i2 = dao.getWorkflowInstance(subWorkflowId1, emptySet(), null);
    assertThat(i2.parentWorkflowId, equalTo(parentWorkflowId));
    assertThat(i2.parentActionId, equalTo(parentActionId));

    WorkflowInstance parent = dao.getWorkflowInstance(parentWorkflowId, EnumSet.of(CHILD_WORKFLOW_IDS), null);
    assertThat(parent.childWorkflows.get(parentActionId), containsInAnyOrder(subWorkflowId1, subWorkflowId2));
  }

  @Test
  public void wakeUpWorkflowExternallyWorksWithEmptyExpectedStates() {
    DateTime now = now();
    DateTime scheduled = now.plusDays(1);
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(scheduled).build();
    long parentWorkflowId = dao.insertWorkflowInstance(i1);
    assertThat(parentWorkflowId, not(equalTo(-1)));
    WorkflowInstance createdWorkflow = dao.getWorkflowInstance(parentWorkflowId, emptySet(), null);

    assertThat(createdWorkflow.nextActivation, equalTo(scheduled));

    long parentActionId = addWorkflowAction(parentWorkflowId, i1, now, now.plusMillis(100));
    assertThat(parentActionId, not(equalTo(-1)));

    long subWorkflowId = addSubWorkflow(parentWorkflowId, parentActionId);
    WorkflowInstance i2 = dao.getWorkflowInstance(subWorkflowId, emptySet(), null);
    assertThat(subWorkflowId, not(equalTo(-1)));
    assertThat(i2.parentWorkflowId, equalTo(parentWorkflowId));
    assertThat(i2.parentActionId, equalTo(parentActionId));

    dao.wakeUpWorkflowExternally(parentWorkflowId, new ArrayList<String>());
    WorkflowInstance wakenWorkflow = dao.getWorkflowInstance(parentWorkflowId, emptySet(), null);
    assertTrue(wakenWorkflow.nextActivation.isBefore(now.plusMinutes(1)));
  }

  @Test
  public void wakeUpWorkflowExternallyWorksWithExpectedStates() {
    DateTime now = now();
    DateTime scheduled = now.plusDays(1);
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(scheduled).build();
    long parentWorkflowId = dao.insertWorkflowInstance(i1);
    assertThat(parentWorkflowId, not(equalTo(-1)));
    WorkflowInstance createdWorkflow = dao.getWorkflowInstance(parentWorkflowId, emptySet(), null);

    assertThat(createdWorkflow.nextActivation, equalTo(scheduled));

    long parentActionId = addWorkflowAction(parentWorkflowId, i1, now, now.plusMillis(100));
    assertThat(parentActionId, not(equalTo(-1)));

    long subWorkflowId = addSubWorkflow(parentWorkflowId, parentActionId);
    WorkflowInstance i2 = dao.getWorkflowInstance(subWorkflowId, emptySet(), null);
    assertThat(subWorkflowId, not(equalTo(-1)));
    assertThat(i2.parentWorkflowId, equalTo(parentWorkflowId));
    assertThat(i2.parentActionId, equalTo(parentActionId));

    dao.wakeUpWorkflowExternally(parentWorkflowId, asList("CreateLoan"));
    WorkflowInstance wakenWorkflow = dao.getWorkflowInstance(parentWorkflowId, emptySet(), null);
    assertTrue(wakenWorkflow.nextActivation.isBefore(now.plusMinutes(1)));
  }

  @Test
  public void wakeUpWorkflowExternallyDoesNotWakeUpWorkflowInUnexpectedState() {
    DateTime now = now();
    DateTime scheduled = now.plusDays(1);
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(scheduled).setState("unexpected").build();
    long parentWorkflowId = dao.insertWorkflowInstance(i1);
    assertThat(parentWorkflowId, not(equalTo(-1)));
    WorkflowInstance createdWorkflow = dao.getWorkflowInstance(parentWorkflowId, emptySet(), null);

    assertThat(createdWorkflow.nextActivation, equalTo(scheduled));

    long parentActionId = addWorkflowAction(parentWorkflowId, i1, now, now.plusMillis(100));
    assertThat(parentActionId, not(equalTo(-1)));

    long subWorkflowId = addSubWorkflow(parentWorkflowId, parentActionId);
    WorkflowInstance i2 = dao.getWorkflowInstance(subWorkflowId, emptySet(), null);
    assertThat(subWorkflowId, not(equalTo(-1)));
    assertThat(i2.parentWorkflowId, equalTo(parentWorkflowId));
    assertThat(i2.parentActionId, equalTo(parentActionId));

    dao.wakeUpWorkflowExternally(parentWorkflowId, asList("CreateLoan"));
    WorkflowInstance wakenWorkflow = dao.getWorkflowInstance(parentWorkflowId, emptySet(), null);
    assertThat(wakenWorkflow.nextActivation, is(scheduled));
  }

  @Test
  public void recoverWorkflowInstancesFromDeadNodesSetsExecutorIdToNullAndStatusToInProgressAndInsertsAction() {
    int crashedExecutorId = 999;
    insertCrashedExecutor(crashedExecutorId, executorDao.getExecutorGroup());
    long id = dao.insertWorkflowInstance(
        new WorkflowInstance.Builder().setType("test").setExternalId("extId").setExecutorGroup(executorDao.getExecutorGroup())
            .setStatus(executing).setState("processing").setPriority((short) 0).build());
    int updated = jdbc.update("update nflow_workflow set executor_id = ? where id = ?", crashedExecutorId, id);
    assertThat(updated, is(1));

    dao.recoverWorkflowInstancesFromDeadNodes();

    Integer executorId = jdbc.queryForObject("select executor_id from nflow_workflow where id = ?", Integer.class, id);
    assertThat(executorId, is(nullValue()));
    String status = jdbc.queryForObject("select status from nflow_workflow where id = ?", String.class, id);
    assertThat(status, is(inProgress.name()));

    List<WorkflowInstanceAction.Builder> actions = jdbc.query("select * from nflow_workflow_action where workflow_id = ?",
        new WorkflowInstanceActionRowMapper(sqlVariant), id);
    assertThat(actions.size(), is(1));
    WorkflowInstanceAction workflowInstanceAction = actions.get(0).build();
    assertThat(workflowInstanceAction.executorId, is(executorDao.getExecutorId()));
    assertThat(workflowInstanceAction.type, is(recovery));
    assertThat(workflowInstanceAction.stateText, is("Recovered"));

    dao.recoverWorkflowInstancesFromDeadNodes();

    executorId = jdbc.queryForObject("select executor_id from nflow_workflow where id = ?", Integer.class, id);
    assertThat(executorId, is(nullValue()));

    actions = jdbc.query("select * from nflow_workflow_action where workflow_id = ?",
        new WorkflowInstanceActionRowMapper(sqlVariant), id);
    assertThat(actions.size(), is(1));
    assertThat(workflowInstanceAction.executorId, is(executorDao.getExecutorId()));
    assertThat(workflowInstanceAction.type, is(recovery));
    assertThat(workflowInstanceAction.stateText, is("Recovered"));
  }

  @Test
  public void settingSignalInsertsAction() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setBusinessKey("setSignalTest").build();
    long instanceId = dao.insertWorkflowInstance(i);

    dao.setSignal(instanceId, Optional.of(42), "testing", WorkflowActionType.externalChange);

    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().setBusinessKey("setSignalTest").setIncludeActions(true)
        .build();
    i = dao.queryWorkflowInstances(q).get(0);
    assertThat(i.signal, is(Optional.of(42)));
    assertThat(i.actions.size(), is(1));
    WorkflowInstanceAction action = i.actions.get(0);
    assertThat(action.stateText, is("testing"));
    assertThat(action.type, is(WorkflowActionType.externalChange));
    assertThat(dao.getSignal(instanceId), is(Optional.of(42)));
  }

  @Test
  public void clearingSignalInsertsAction() {
    WorkflowInstance i = constructWorkflowInstanceBuilder().setBusinessKey("clearSignalTest").build();
    long instanceId = dao.insertWorkflowInstance(i);

    dao.setSignal(instanceId, Optional.of(42), "testing", WorkflowActionType.externalChange);

    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().setBusinessKey("clearSignalTest").setIncludeActions(true)
        .build();
    i = dao.queryWorkflowInstances(q).get(0);
    assertThat(i.signal, is(Optional.of(42)));
    assertThat(i.actions.size(), is(1));
    assertThat(dao.getSignal(instanceId), is(Optional.of(42)));

    dao.setSignal(instanceId, Optional.empty(), "cleared", WorkflowActionType.externalChange);

    q = new QueryWorkflowInstances.Builder().setBusinessKey("clearSignalTest").setIncludeActions(true).build();
    i = dao.queryWorkflowInstances(q).get(0);
    assertThat(i.signal, is(Optional.empty()));
    assertThat(i.actions.size(), is(2));
    WorkflowInstanceAction action = i.actions.get(0);
    assertThat(action.stateText, is("cleared"));
    assertThat(action.type, is(WorkflowActionType.externalChange));
    assertThat(dao.getSignal(instanceId), is(Optional.empty()));
  }

  private static void checkSameWorkflowInfo(WorkflowInstance i1, WorkflowInstance i2) {
    assertThat(i1.type, equalTo(i2.type));
    assertThat(i1.executorId, equalTo(i2.executorId));
    assertThat(i1.state, equalTo(i2.state));
    assertThat(i1.stateText, equalTo(i2.stateText));
    assertThat(i1.nextActivation, equalTo(i2.nextActivation));
    assertThat(i1.stateVariables.size(), equalTo(i2.stateVariables.size()));
    Map<String, String> tmpVars = new LinkedHashMap<>(i1.stateVariables);
    for (Entry<String, String> entry : tmpVars.entrySet()) {
      assertTrue(i2.stateVariables.containsKey(entry.getKey()));
      assertThat(i2.stateVariables.get(entry.getKey()), equalTo(entry.getValue()));
    }
    for (Entry<Long, List<Long>> entry : i1.childWorkflows.entrySet()) {
      Long key = entry.getKey();
      assertTrue(i2.childWorkflows.containsKey(key));
      assertThat(i2.childWorkflows.get(key), is(entry.getValue()));
    }
    assertThat(i1.signal, equalTo(i2.signal));
    assertThat(i1.started, equalTo(i2.started));
  }

  private long addWorkflowAction(long workflowId, final WorkflowInstance instance, DateTime started, DateTime ended) {
    final WorkflowInstanceAction action = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
        .setExecutionEnd(ended).setRetryNo(1).setType(stateExecution).setState("test").setStateText("state text")
        .setWorkflowInstanceId(workflowId).build();
    long actionId = transaction.execute((TransactionCallback<Long>) status -> dao.insertWorkflowInstanceAction(instance, action));
    return actionId;
  }

  private long addSubWorkflow(long parentWorkflowId, long parentActionId) {
    final WorkflowInstance subWorkflow = constructWorkflowInstanceBuilder().setParentWorkflowId(parentWorkflowId)
        .setParentActionId(parentActionId).build();
    return dao.insertWorkflowInstance(subWorkflow);
  }

  static class Poller implements Runnable {
    final WorkflowInstanceDao dao;
    final int batchSize;
    boolean detectedRaceCondition = false;
    int returnSize = MAX_VALUE;

    public Poller(WorkflowInstanceDao dao, int batchSize) {
      this.dao = dao;
      this.batchSize = batchSize;
    }

    @Override
    public void run() {
      try {
        returnSize = min(returnSize, dao.pollNextWorkflowInstanceIds(batchSize).size());
      } catch (PollingRaceConditionException ex) {
        ex.printStackTrace();
        returnSize = 0;
        detectedRaceCondition = true;
      }
    }
  }

  @Test
  public void checkStateVariableValueWorks() {
    dao.checkStateVariableValueLength("foo", repeat('a', dao.getStateVariableValueMaxLength()));
  }

  @Test
  public void checkStateVariableValueThrowsExceptionWhenValueIsTooLong() {
    assertThrows(StateVariableValueTooLongException.class,
        () -> dao.checkStateVariableValueLength("foo", repeat('a', dao.getStateVariableValueMaxLength() + 1)));
  }
}
