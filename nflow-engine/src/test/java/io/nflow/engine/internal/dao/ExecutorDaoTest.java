package io.nflow.engine.internal.dao;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;

import io.nflow.engine.workflow.executor.WorkflowExecutor;
import org.junit.jupiter.api.Test;

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
    assertThat(executor.stopped, is(nullValue()));
  }

  @Test
  public void markShutdownSetsExecutorExpired() {
    jdbc.update(
        "insert into nflow_executor (id, host, pid, executor_group, started, active, expires) values (?, ?, ?, ?, ?, ?, ?)",
        dao.getExecutorId(), "localhost", 666, dao.getExecutorGroup(), now().toDate(), now().toDate(),
        now().plusHours(1).toDate());

    dao.markShutdown();

    WorkflowExecutor executor = dao.getExecutors().get(0);
    assertThat(executor.expires.isAfterNow(), is(false));
    assertThat(executor.stopped, is(notNullValue()));
    assertThat(executor.stopped.isAfterNow(), is(false));
  }
}
