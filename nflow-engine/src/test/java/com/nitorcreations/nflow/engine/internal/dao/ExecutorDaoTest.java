package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.inProgress;
import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.recovery;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao.WorkflowInstanceActionRowMapper;
import com.nitorcreations.nflow.engine.workflow.executor.WorkflowExecutor;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class ExecutorDaoTest extends BaseDaoTest {

  @Inject
  ExecutorDao dao;
  @Inject
  WorkflowInstanceDao workflowInstanceDao;

  private final DateTime started = now().minusDays(1);

  @Test
  public void tickCausesDeadNodeRecoveryPeriodically() {
    DateTime firstNextUpdate = dao.getMaxWaitUntil();
    dao.tick();
    DateTime secondNextUpdate = dao.getMaxWaitUntil();
    assertNotEquals(firstNextUpdate, secondNextUpdate);
    dao.tick();
    assertEquals(secondNextUpdate, dao.getMaxWaitUntil());
  }

  @Test
  public void recoverWorkflowInstancesFromDeadNodesSetsExecutorIdToNullAndInsertsAction() {
    int crashedExecutorId = 999;
    insertCrashedExecutor(crashedExecutorId);
    int id = workflowInstanceDao.insertWorkflowInstance(new WorkflowInstance.Builder().setType("test").setExternalId("extId")
        .setExecutorGroup(dao.getExecutorGroup()).setStatus(inProgress).setState("processing").build());
    int updated = jdbc.update("update nflow_workflow set executor_id = ? where id = ?", crashedExecutorId, id);
    assertThat(updated, is(1));

    dao.recoverWorkflowInstancesFromDeadNodes();

    Integer executorId = jdbc.queryForObject("select executor_id from nflow_workflow where id = ?", Integer.class, id);
    assertThat(executorId, is(nullValue()));

    List<WorkflowInstanceAction> actions = jdbc.query("select * from nflow_workflow_action where workflow_id = ?",
        new WorkflowInstanceActionRowMapper(Collections.<Integer,Map<String, String>>emptyMap()), id);
    assertThat(actions.size(), is(1));
    WorkflowInstanceAction workflowInstanceAction = actions.get(0);
    assertThat(workflowInstanceAction.executorId, is(dao.getExecutorId()));
    assertThat(workflowInstanceAction.type, is(recovery));
    assertThat(workflowInstanceAction.stateText, is("Recovered"));

    dao.recoverWorkflowInstancesFromDeadNodes();

    executorId = jdbc.queryForObject("select executor_id from nflow_workflow where id = ?", Integer.class, id);
    assertThat(executorId, is(nullValue()));

    actions = jdbc.query("select * from nflow_workflow_action where workflow_id = ?", new WorkflowInstanceActionRowMapper(
        Collections.<Integer, Map<String, String>> emptyMap()), id);
    assertThat(actions.size(), is(1));
    assertThat(workflowInstanceAction.executorId, is(dao.getExecutorId()));
    assertThat(workflowInstanceAction.type, is(recovery));
    assertThat(workflowInstanceAction.stateText, is("Recovered"));
  }

  private void insertCrashedExecutor(int crashedExecutorId) {
    jdbc.update(
        "insert into nflow_executor (id, host, pid, executor_group, started, active, expires) values (?, ?, ?, ?, ?, ?, ?)",
        crashedExecutorId, "localhost", 666, dao.getExecutorGroup(), started.toDate(), started.plusSeconds(1).toDate(), started
            .plusHours(1).toDate());
  }

  @Test
  public void getExecutorsWorks() {
    insertCrashedExecutor(1);

    List<WorkflowExecutor> executors = dao.getExecutors();

    assertThat(executors.size(), is(1));
    WorkflowExecutor executor = executors.get(0);
    assertThat(executor.id, is(1));
    assertThat(executor.host, is("localhost"));
    assertThat(executor.pid, is(666));
    assertThat(executor.executorGroup, is(dao.getExecutorGroup()));
    assertThat(executor.started, is(started));
    assertThat(executor.active, is(started.plusSeconds(1)));
    assertThat(executor.expires, is(started.plusHours(1)));
  }
}
