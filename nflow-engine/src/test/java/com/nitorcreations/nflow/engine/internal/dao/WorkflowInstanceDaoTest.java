package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.executing;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.manual;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.paused;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.externalChange;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.min;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.joda.time.DateTime;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.nitorcreations.nflow.engine.internal.storage.db.PgDatabaseConfiguration.PostgreSQLVariants;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class WorkflowInstanceDaoTest extends BaseDaoTest {

  @Inject
  WorkflowInstanceDao dao;
  @Inject
  TransactionTemplate transaction;
  List<WorkflowInstance> noChildWorkflows = Arrays.<WorkflowInstance>asList();

  @Test
  public void roundTripTest() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("a", "1");
    int id = dao.insertWorkflowInstance(i1);
    WorkflowInstance i2 = dao.getWorkflowInstance(id);
    assertThat(i2.id, notNullValue());
    assertThat(i2.created, notNullValue());
    assertThat(i2.modified, notNullValue());
    checkSameWorkflowInfo(i1, i2);
  }

  @Test
  public void queryWorkflowInstanceWithAllConditions() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("b", "2");
    int workflowId = dao.insertWorkflowInstance(i1);
    assertThat(workflowId, not(equalTo(-1)));

    WorkflowInstanceAction action = constructActionBuilder(workflowId).build();
    int actionId = dao.insertWorkflowInstanceAction(action);

    WorkflowInstance child = constructWorkflowInstanceBuilder().setParentWorkflowId(workflowId)
            .setParentActionId(actionId).build();
    int childId = dao.insertWorkflowInstance(child);
    assertThat(childId, not(equalTo(-1)));

    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().addIds(childId).addTypes(child.type)
        .addStates(child.state).addStatuses(i1.status).setParentWorkflowId(workflowId).setParentActionId(actionId)
        .setBusinessKey(child.businessKey).setExternalId(child.externalId).setIncludeActions(true)
        .setIncludeActionStateVariables(true).setIncludeCurrentStateVariables(true).build();
    List<WorkflowInstance> l = dao.queryWorkflowInstances(q);
    assertThat(l.size(), is(1));
    checkSameWorkflowInfo(child, l.get(0));
  }

  @Test
  public void queryWorkflowInstanceWithMinimalConditions() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(i1);
    assertThat(id, not(equalTo(-1)));
    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().build();
    List<WorkflowInstance> createdInstances = dao.queryWorkflowInstances(q);
    assertThat(createdInstances.size(), is(1));
    WorkflowInstance instance = createdInstances.get(0);
    checkSameWorkflowInfo(i1, instance);
    assertNull(instance.started);
  }

  @Test
  public void updateWorkflowInstance() throws InterruptedException {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setStatus(created).build();
    int id = dao.insertWorkflowInstance(i1);
    List<Integer> ids = dao.pollNextWorkflowInstanceIds(1);
    assertThat(ids, contains(id));
    final WorkflowInstance i2 = new WorkflowInstance.Builder(dao.getWorkflowInstance(id)).setStatus(inProgress)
        .setState("updateState").setStateText("update text").setNextActivation(DateTime.now()).build();
    final WorkflowInstance polledInstance = dao.getWorkflowInstance(id);
    assertThat(polledInstance.status, equalTo(executing));
    final DateTime originalModifiedTime = polledInstance.modified;
    sleep(1);
    DateTime started = DateTime.now();
    WorkflowInstanceAction a1 = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
        .setExecutionEnd(DateTime.now().plusMillis(100)).setRetryNo(1).setState("test").setStateText("state text")
        .setWorkflowInstanceId(id).setType(stateExecution).build();
    dao.updateWorkflowInstanceAfterExecution(i2, a1, noChildWorkflows);
    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("status"), equalTo(inProgress.name()));
        assertThat(rs.getString("state"), equalTo(i2.state));
        assertThat(rs.getString("state_text"), equalTo(i2.stateText));
        assertThat(rs.getTimestamp("next_activation").getTime(), equalTo(i2.nextActivation.toDate().getTime()));
        assertThat(rs.getInt("executor_id") != 0, equalTo(i2.status == executing));
        assertThat(rs.getTimestamp("modified").getTime(), greaterThan(originalModifiedTime.getMillis()));
      }
    });
  }

  @Test
  public void updateNotRunningWorkflowInstanceUpdatesStatusForNotRunningInstance() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
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
      }
    });
  }

  @Test
  public void stopWorkflowInstanceWorks() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
    final DateTime originalModifiedTime = dao.getWorkflowInstance(id).modified;
    boolean updated = dao.stopNotRunningWorkflowInstance(id, "reason");
    assertThat(updated, is(true));
    JdbcTemplate template = new JdbcTemplate(ds);
    template.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("state"), equalTo(instance.state));
        assertThat(rs.getString("state_text"), equalTo("reason"));
        assertThat(rs.getTimestamp("next_activation"), is(nullValue()));
        assertThat(rs.getInt("executor_id"), equalTo(0));
        assertThat(rs.getTimestamp("modified").getTime(), greaterThan(originalModifiedTime.getMillis()));
        assertThat(rs.getString("status"), equalTo("stopped"));
      }
    });
  }

  @Test
  public void stopWorkflowInstanceDoesNotUpdateExecutingWorkflow() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
    jdbc.update("update nflow_workflow set executor_id = 1 where id = ?", id);
    final DateTime originalModifiedTime = dao.getWorkflowInstance(id).modified;
    boolean updated = dao.stopNotRunningWorkflowInstance(id, "reason");
    assertThat(updated, is(false));
    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("state"), equalTo(instance.state));
        assertThat(rs.getString("state_text"), equalTo(instance.stateText));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(instance.nextActivation.getMillis()));
        assertThat(rs.getInt("executor_id"), equalTo(1));
        assertThat(rs.getTimestamp("modified").getTime(), equalTo(originalModifiedTime.getMillis()));
        assertThat(rs.getString("status"), equalTo(instance.status.name()));
      }
    });
  }

  @Test
  public void pauseWorkflowInstanceWorks() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
    final DateTime originalModifiedTime = dao.getWorkflowInstance(id).modified;
    boolean updated = dao.pauseNotRunningWorkflowInstance(id, "reason");
    assertThat(updated, is(true));
    JdbcTemplate template = new JdbcTemplate(ds);
    template.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("state"), equalTo(instance.state));
        assertThat(rs.getString("state_text"), equalTo("reason"));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(instance.nextActivation.getMillis()));
        assertThat(rs.getInt("executor_id"), equalTo(0));
        assertThat(rs.getTimestamp("modified").getTime(), greaterThan(originalModifiedTime.getMillis()));
        assertThat(rs.getString("status"), equalTo("paused"));
      }
    });
  }

  @Test
  public void pauseWorkflowInstanceDoesNotUpdateExecutingWorkflow() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
    jdbc.update("update nflow_workflow set executor_id = 1 where id = ?", id);
    final DateTime originalModifiedTime = dao.getWorkflowInstance(id).modified;
    boolean updated = dao.pauseNotRunningWorkflowInstance(id, "reason");
    assertThat(updated, is(false));
    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("state"), equalTo(instance.state));
        assertThat(rs.getString("state_text"), equalTo(instance.stateText));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(instance.nextActivation.getMillis()));
        assertThat(rs.getInt("executor_id"), equalTo(1));
        assertThat(rs.getTimestamp("modified").getTime(), equalTo(originalModifiedTime.getMillis()));
        assertThat(rs.getString("status"), equalTo(instance.status.name()));
      }
    });
  }

  @Test
  public void resumeWorkflowInstanceWorks() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(paused).build();
    int id = dao.insertWorkflowInstance(instance);
    final DateTime originalModifiedTime = dao.getWorkflowInstance(id).modified;
    boolean updated = dao.resumePausedWorkflowInstance(id, "reason");
    assertThat(updated, is(true));
    JdbcTemplate template = new JdbcTemplate(ds);
    template.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("state"), equalTo(instance.state));
        assertThat(rs.getString("state_text"), equalTo("reason"));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(instance.nextActivation.getMillis()));
        assertThat(rs.getInt("executor_id"), equalTo(0));
        assertThat(rs.getTimestamp("modified").getTime(), greaterThan(originalModifiedTime.getMillis()));
        assertThat(rs.getString("status"), equalTo("inProgress"));
      }
    });
  }

  @Test
  public void resumeWorkflowInstanceDoesNotUpdateRunningWorkflow() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
    final DateTime originalModifiedTime = dao.getWorkflowInstance(id).modified;
    boolean updated = dao.resumePausedWorkflowInstance(id, "reason");
    assertThat(updated, is(false));
    jdbc.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("state"), equalTo(instance.state));
        assertThat(rs.getString("state_text"), equalTo(instance.stateText));
        assertThat(rs.getTimestamp("next_activation").getTime(), is(instance.nextActivation.getMillis()));
        assertThat(rs.getInt("executor_id"), equalTo(0));
        assertThat(rs.getTimestamp("modified").getTime(), equalTo(originalModifiedTime.getMillis()));
        assertThat(rs.getString("status"), equalTo(instance.status.name()));
      }
    });
  }

  @Test
  public void updateNotRunningWorkflowInstanceUpdatesStateForNotRunningInstance() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
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
      }
    });
  }

  @Test
  public void updateNotRunningWorkflowInstanceUpdatesNextActivationForNotRunningInstance() {
    final WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
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
      }
    });
  }

  @Test
  public void updateNotRunningWorkflowInstanceDoesNotUpdateRunningInstance() {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(instance);
    assertThat(jdbc.update("update nflow_workflow set executor_id = 1 where id = ?", id), is(1));
    final DateTime tomorrow = now().plusDays(1);
    WorkflowInstance modifiedInstance = new WorkflowInstance.Builder(instance).setId(id).setState("manualState")
        .setNextActivation(tomorrow).setStatus(manual).build();
    boolean updated = dao.updateNotRunningWorkflowInstance(modifiedInstance);
    assertThat(updated, is(false));
  }

  @Test
  public void updatingNextActivationToNullWhileExternalNextActivationIsNotNull() {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    int workflowId = dao.insertWorkflowInstance(instance);
    assertTrue(workflowId > -1);
    jdbc.update("update nflow_workflow set executor_id = 1 where id = ?", workflowId);

    assertThat(jdbc.update("update nflow_workflow set external_next_activation = ? where id = ?",
            new Timestamp(DateTime.now().getMillis()), workflowId), is(1));
    dao.updateWorkflowInstance(new WorkflowInstance.Builder(dao.getWorkflowInstance(workflowId)).setNextActivation(null).build());
    WorkflowInstance updated = dao.getWorkflowInstance(workflowId);
    assertThat(updated.nextActivation, is(CoreMatchers.nullValue()));
  }

  @Test
  public void updatingNextActivationWhenExternalNextActivationIsEarlier() {
    DateTime now = DateTime.now();
    DateTime future = now.plusDays(1);

    WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(future).build();

    final int workflowId = dao.insertWorkflowInstance(instance);
    jdbc.update("update nflow_workflow set executor_id = 1 where id = ?", workflowId);
    assertTrue(workflowId > -1);
    assertThat(jdbc.update("update nflow_workflow set external_next_activation = ? where id = ?",
            new Timestamp(now.getMillis()), workflowId), is(1));
    System.out.println(jdbc.queryForList("select * from nflow_workflow where id = " + workflowId));

    final WorkflowInstance updatedInstance = new WorkflowInstance.Builder(dao.getWorkflowInstance(workflowId)).build();

    WorkflowInstanceAction action = constructActionBuilder(workflowId).build();
    dao.updateWorkflowInstanceAfterExecution(updatedInstance, action, Collections.<WorkflowInstance>emptyList());

    WorkflowInstance updated = dao.getWorkflowInstance(workflowId);
    System.out.println(updated);
    System.out.println(jdbc.queryForList("select * from nflow_workflow where id = " + workflowId));
    assertThat(updated.nextActivation, is(now));
  }

  @Test
  public void updatingNextActivationWhenExternalNextActivationIsLater() {
    DateTime now = DateTime.now();
    DateTime future = now.plusDays(1);

    WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(now).build();
    int workflowId = dao.insertWorkflowInstance(instance);
    jdbc.update("update nflow_workflow set executor_id = 1 where id = ?", workflowId);

    assertTrue(workflowId > -1);
    assertThat(jdbc.update("update nflow_workflow set external_next_activation = ? where id = ?",
            new Timestamp(future.getMillis()), workflowId), is(1));
    dao.updateWorkflowInstance(new WorkflowInstance.Builder(dao.getWorkflowInstance(workflowId)).build());
    WorkflowInstance updated = dao.getWorkflowInstance(workflowId);
    assertThat(updated.nextActivation, is(now));
  }

  @Test
  public void fakePostgreSQLupdateWorkflowInstance() {
    JdbcTemplate j = mock(JdbcTemplate.class);
    WorkflowInstanceDao d = preparePostgreSQLDao(j);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Object> args = ArgumentCaptor.forClass(Object.class);
    when(j.queryForObject(sql.capture(), eq(Integer.class), args.capture())).thenReturn(42);

    DateTime started = DateTime.now();
    WorkflowInstance i2 = new WorkflowInstance.Builder().setState("updateState").setStateText("update text")
        .setNextActivation(started.plusSeconds(1)).setStatus(executing).setRetries(3).setId(43).putStateVariable("A", "B")
        .build();
    WorkflowInstanceAction a1 = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(4)
        .setExecutionEnd(started.plusMillis(100)).setRetryNo(1).setType(externalChange).setState("test")
        .setStateText("state text").setWorkflowInstanceId(43).build();

    d.updateWorkflowInstanceAfterExecution(i2, a1, noChildWorkflows);
    assertEquals(
        "with wf as (update nflow_workflow set status = ?::workflow_status, state = ?, state_text = ?, next_activation = (case when ?::timestamptz is null then null when external_next_activation is null then ?::timestamptz when ?::timestamptz < external_next_activation then ?::timestamptz else external_next_activation end), external_next_activation = null, executor_id = ?, retries = ? where id = ? and executor_id = 42 returning id), act as (insert into nflow_workflow_action(workflow_id, executor_id, type, state, state_text, retry_no, execution_start, execution_end) select wf.id,?,?::action_type,?,?,?,?,? from wf returning id), ins17 as (insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value) select wf.id,act.id,?,? from wf,act) select act.id from act",
        sql.getValue());
    assertThat(args.getAllValues().size(), is(countMatches(sql.getValue(), "?")));

    int i = 0;
    assertThat(args.getAllValues().get(i++), is((Object) i2.status.name()));
    assertThat(args.getAllValues().get(i++), is((Object) i2.state));
    assertThat(args.getAllValues().get(i++), is((Object) i2.stateText));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(i2.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(i2.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(i2.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(i2.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) 42));
    assertThat(args.getAllValues().get(i++), is((Object) i2.retries));
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
    when(j.queryForObject(sql.capture(), eq(Integer.class), args.capture())).thenReturn(42);

    DateTime started = DateTime.now();
    WorkflowInstance wf = new WorkflowInstance.Builder().setStatus(inProgress).setState("updateState")
        .setStateText("update text").setParentWorkflowId(110).setParentActionId(421)
        .setNextActivation(started.plusSeconds(1)).setRetries(3).setId(43).putStateVariable("A", "B")
        .putStateVariable("C", "D").build();

    d.insertWorkflowInstance(wf);
    assertEquals(
            "with wf as (insert into nflow_workflow(type, parent_workflow_id, parent_action_id, business_key, external_id, executor_group, status, state, state_text, next_activation) values (?, ?, ?, ?, ?, ?, ?::workflow_status, ?, ?, ?) returning id), ins10 as (insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value) select wf.id,0,?,? from wf), ins12 as (insert into nflow_workflow_state(workflow_id, action_id, state_key, state_value) select wf.id,0,?,? from wf) select wf.id from wf",
            sql.getValue());
    assertThat(args.getAllValues().size(), is(countMatches(sql.getValue(), "?")));

    int i = 0;
    assertThat(args.getAllValues().get(i++), is((Object) wf.type));
    assertThat(args.getAllValues().get(i++), is((Object) wf.parentWorkflowId));
    assertThat(args.getAllValues().get(i++), is((Object) wf.parentActionId));
    assertThat(args.getAllValues().get(i++), is((Object) wf.businessKey));
    assertThat(args.getAllValues().get(i++), is((Object) wf.externalId));
    assertThat(args.getAllValues().get(i++), is((Object) wf.executorGroup));
    assertThat(args.getAllValues().get(i++), is((Object) wf.status.name()));
    assertThat(args.getAllValues().get(i++), is((Object) wf.state));
    assertThat(args.getAllValues().get(i++), is((Object) wf.stateText));
    assertThat(args.getAllValues().get(i++), is((Object) new Timestamp(wf.nextActivation.getMillis())));
    assertThat(args.getAllValues().get(i++), is((Object) "A"));
    assertThat(args.getAllValues().get(i++), is((Object) "B"));
    assertThat(args.getAllValues().get(i++), is((Object) "C"));
    assertThat(args.getAllValues().get(i++), is((Object) "D"));
  }

  @Test
  public void insertWorkflowInstanceActionWorks() {
    final WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("a", "1");
    int id = dao.insertWorkflowInstance(i1);
    DateTime started = DateTime.now();
    final WorkflowInstanceAction a1 = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
        .setExecutionEnd(DateTime.now().plusMillis(100)).setRetryNo(1).setType(stateExecution).setState("test")
        .setStateText("state text")
        .setWorkflowInstanceId(id).build();
    i1.stateVariables.put("b", "2");
    transaction.execute(new TransactionCallback<Void>() {
      @Override
      public Void doInTransaction(TransactionStatus status) {
        dao.insertWorkflowInstanceAction(i1, a1);
        return null;
      }
    });
    WorkflowInstance createdInstance = dao.getWorkflowInstance(id);
    checkSameWorkflowInfo(i1, createdInstance);
  }

  @Test
  public void pollNextWorkflowInstances() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(DateTime.now().minusMinutes(1))
        .setExecutorGroup("junit").build();
    int id = dao.insertWorkflowInstance(i1);
    List<Integer> firstBatch = dao.pollNextWorkflowInstanceIds(100);
    List<Integer> secondBatch = dao.pollNextWorkflowInstanceIds(100);
    assertThat(firstBatch.size(), equalTo(1));
    assertThat(firstBatch.get(0), equalTo(id));
    assertThat(secondBatch.size(), equalTo(0));
  }

  @Test
  public void fakePostgreSQLpollNextWorkflowInstances() {
    JdbcTemplate j = mock(JdbcTemplate.class);
    WorkflowInstanceDao d = preparePostgreSQLDao(j);
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    when(j.queryForList(sql.capture(), eq(Integer.class))).thenReturn(asList(1, 2, 3));
    assertThat(d.pollNextWorkflowInstanceIds(5), is(asList(1, 2, 3)));
    assertEquals(
            "update nflow_workflow set executor_id = 42, status = 'executing', external_next_activation = null where id in (select id from nflow_workflow where executor_id is null and status in ('created', 'inProgress') and next_activation < current_timestamp and group matches order by next_activation asc limit 5) and executor_id is null returning id",
            sql.getValue());
  }

  private WorkflowInstanceDao preparePostgreSQLDao(JdbcTemplate j) {
    WorkflowInstanceDao d = new WorkflowInstanceDao();
    d.setSQLVariants(new PostgreSQLVariants());
    ExecutorDao eDao = mock(ExecutorDao.class);
    when(eDao.getExecutorGroupCondition()).thenReturn("group matches");
    when(eDao.getExecutorId()).thenReturn(42);
    d.setExecutorDao(eDao);
    d.setJdbcTemplate(j);
    d.instanceStateTextLength = 128;
    d.actionStateTextLength = 128;
    return d;
  }

  @Test
  public void pollNextWorkflowInstancesWithPartialRaceCondition() throws InterruptedException {
    int batchSize = 100;
    for (int i = 0; i < batchSize; i++) {
      WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(DateTime.now().minusMinutes(1))
          .setExecutorGroup("junit").build();
      dao.insertWorkflowInstance(instance);
    }
    Poller[] pollers = new Poller[] { new Poller(dao, batchSize), new Poller(dao, batchSize) };
    Thread[] threads = new Thread[] { new Thread(pollers[0]), new Thread(pollers[1]) };
    threads[0].start();
    threads[1].start();
    threads[0].join();
    threads[1].join();
    assertThat(pollers[0].returnSize + pollers[1].returnSize, is(batchSize));
    assertTrue("Race condition should happen", pollers[0].detectedRaceCondition || pollers[1].detectedRaceCondition
        || (pollers[0].returnSize < batchSize && pollers[1].returnSize < batchSize));
  }

  @Test
  public void wakesUpSleepingWorkflow() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    int id = dao.insertWorkflowInstance(i1);
    assertThat(dao.getWorkflowInstance(id).nextActivation, nullValue());
    dao.wakeupWorkflowInstanceIfNotExecuting(id, new String[0]);
    assertThat(dao.getWorkflowInstance(id).nextActivation, notNullValue());
  }

  @Test
  public void doesNotWakeUpRunningWorkflow() {
    DateTime past = now().minusDays(1);
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setExecutorGroup("junit").setNextActivation(past).build();
    int id = dao.insertWorkflowInstance(i1);
    List<Integer> ids = dao.pollNextWorkflowInstanceIds(1);
    assertThat(ids, contains(id));
    assertThat(dao.getWorkflowInstance(id).nextActivation, is(past));
    dao.wakeupWorkflowInstanceIfNotExecuting(id, new String[] { i1.state });
    assertThat(dao.getWorkflowInstance(id).nextActivation, is(past));
  }

  @Test
  public void wakesUpWorkflowInMatchingState() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    int id = dao.insertWorkflowInstance(i1);
    assertThat(dao.getWorkflowInstance(id).nextActivation, nullValue());
    dao.wakeupWorkflowInstanceIfNotExecuting(id, new String[] { "otherState", i1.state });
    assertThat(dao.getWorkflowInstance(id).nextActivation, notNullValue());
  }

  @Test
  public void getWorkflowInstanceStateWorks() {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().build();
    int workflowInstanceId = dao.insertWorkflowInstance(instance);

    String state = dao.getWorkflowInstanceState(workflowInstanceId);

    assertThat(state, is("CreateLoan"));
  }

  @Test
  public void insertingSubWorkflowWorks() {
    final WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("b", "2");
    int parentWorkflowId = dao.insertWorkflowInstance(i1);
    assertThat(parentWorkflowId, not(equalTo(-1)));

    int parentActionId = addWorkflowAction(parentWorkflowId, i1);
    WorkflowInstance createdInstance = dao.getWorkflowInstance(parentWorkflowId);
    checkSameWorkflowInfo(i1, createdInstance);

    int subWorkflowId = addSubWorkflow(parentWorkflowId, parentActionId);
    assertThat(subWorkflowId, not(equalTo(-1)));

    WorkflowInstance i2 = dao.getWorkflowInstance(subWorkflowId);
    assertThat(i2.parentWorkflowId, equalTo(parentWorkflowId));
    assertThat(i2.parentActionId, equalTo(parentActionId));
  }

  @Test
  public void wakeUpWorkflowExternallyWorks() {
    DateTime now = DateTime.now();
    DateTime scheduled = now.plusDays(1);
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(scheduled).build();
    int parentWorkflowId = dao.insertWorkflowInstance(i1);
    assertThat(parentWorkflowId, not(equalTo(-1)));
    WorkflowInstance createdWorkflow = dao.getWorkflowInstance(parentWorkflowId);

    assertThat(createdWorkflow.nextActivation, equalTo(scheduled));

    int parentActionId = addWorkflowAction(parentWorkflowId, i1);
    assertThat(parentActionId, not(equalTo(-1)));

    int subWorkflowId = addSubWorkflow(parentWorkflowId, parentActionId);
    WorkflowInstance i2 = dao.getWorkflowInstance(subWorkflowId);
    assertThat(subWorkflowId, not(equalTo(-1)));
    assertThat(i2.parentWorkflowId, equalTo(parentWorkflowId));
    assertThat(i2.parentActionId, equalTo(parentActionId));

    dao.wakeUpWorkflowExternally(parentWorkflowId);
    WorkflowInstance wakenWorkflow = dao.getWorkflowInstance(parentWorkflowId);
    assertTrue(wakenWorkflow.nextActivation.isBefore(now.plusMinutes(1)));
  }

  private static void checkSameWorkflowInfo(WorkflowInstance i1, WorkflowInstance i2) {
    assertThat(i1.type, equalTo(i2.type));
    assertThat(i1.executorId, equalTo(i2.executorId));
    assertThat(i1.state, equalTo(i2.state));
    assertThat(i1.stateText, equalTo(i2.stateText));
    assertThat(i1.nextActivation, equalTo(i2.nextActivation));
    assertThat(i1.stateVariables.size(), equalTo(i2.stateVariables.size()));
    Map<String, String> tmpVars = new LinkedHashMap<>(i1.stateVariables);
    for (Map.Entry<String, String> entry : tmpVars.entrySet()) {
      assertTrue(i2.stateVariables.containsKey(entry.getKey()));
      assertThat(i2.stateVariables.get(entry.getKey()), equalTo(entry.getValue()));
    }
  }

  private int addWorkflowAction(int workflowId, final WorkflowInstance instance) {
    DateTime started = DateTime.now();
    final WorkflowInstanceAction action = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
            .setExecutionEnd(DateTime.now().plusMillis(100)).setRetryNo(1).setType(stateExecution).setState("test")
            .setStateText("state text")
            .setWorkflowInstanceId(workflowId).build();
    int actionId = transaction.execute(new TransactionCallback<Integer>() {
      @Override
      public Integer doInTransaction(TransactionStatus status) {
        return dao.insertWorkflowInstanceAction(instance, action);
      }
    });
    return actionId;
  }

  private int addSubWorkflow(int parentWorkflowId, int parentActionId) {
    final WorkflowInstance subWorkflow = constructWorkflowInstanceBuilder().setParentWorkflowId(parentWorkflowId).setParentActionId(parentActionId).build();
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
        detectedRaceCondition = ex.getMessage().startsWith("Race condition");
      }
    }
  }

}
