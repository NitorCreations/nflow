package io.nflow.engine.internal.dao;

import static org.hamcrest.Matchers.is;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Test;

import io.nflow.engine.workflow.executor.WorkflowExecutor;

public class ExecutorDaoTest extends BaseDaoTest {

  @Inject
  ExecutorDao dao;

  @Test
  public void tickCausesDeadNodeRecoveryPeriodically() {
    DateTime firstNextUpdate = dao.getMaxWaitUntil();
    boolean updated = dao.tick();
    assertThat(updated, is(true));
    DateTime secondNextUpdate = dao.getMaxWaitUntil();
    assertNotEquals(firstNextUpdate, secondNextUpdate);
    updated = dao.tick();
    assertThat(updated, is(false));
    assertEquals(secondNextUpdate, dao.getMaxWaitUntil());
  }

  @Test
  public void getExecutorsWorks() {
    insertCrashedExecutor(1, dao.getExecutorGroup());

    List<WorkflowExecutor> executors = dao.getExecutors();

    assertThat(executors.size(), is(1));
    WorkflowExecutor executor = executors.get(0);
    assertThat(executor.id, is(1));
    assertThat(executor.host, is("localhost"));
    assertThat(executor.pid, is(666));
    assertThat(executor.executorGroup, is(dao.getExecutorGroup()));
    assertThat(executor.started, is(crashedNodeStartTime));
    assertThat(executor.active, is(crashedNodeStartTime.plusSeconds(1)));
    assertThat(executor.expires, is(crashedNodeStartTime.plusHours(1)));
  }

  @Test
  public void markShutdownSetsExecutorExpired() {
    jdbc.update(
        "insert into nflow_executor (id, host, pid, executor_group, started, active, expires) values (?, ?, ?, ?, ?, ?, ?)",
        dao.getExecutorId(), "localhost", 666, dao.getExecutorGroup(), now().toDate(), now().toDate(),
        now().plusHours(1).toDate());

    dao.markShutdown();

    assertThat(dao.getExecutors().get(0).expires.isAfterNow(), is(false));
  }
}
