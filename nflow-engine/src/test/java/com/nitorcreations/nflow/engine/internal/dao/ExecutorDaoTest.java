package com.nitorcreations.nflow.engine.internal.dao;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class ExecutorDaoTest extends BaseDaoTest {
  @Inject
  ExecutorDao dao;
  @Inject
  WorkflowInstanceDao workflowInstanceDao;

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
    int currentExecutorId = dao.getExecutorId();
    final int crashedExecutorId = 999;
    JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
    jdbcTemplate.update(
        "insert into nflow_executor (id, host, pid, executor_group, started, active, expires) values (?, ?, ?, ?, ?, ?, ?)",
        crashedExecutorId, "localhost", 666, dao.getExecutorGroup(), new Date(now().minusDays(1).getMillis()), new Date(now().minusDays(1)
            .getMillis()), new Date(now().minusMinutes(1).getMillis()));
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(new PreparedStatementCreator() {
      @Override
      public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement ps;
        int p = 1;
        ps = connection.prepareStatement(
            "insert into nflow_workflow(type, external_id, executor_group, state, executor_id) values (?, ?, ?, ?, ?)", new String[] { "id" });
        ps.setString(p++, "test");
        ps.setString(p++, "extId");
        ps.setString(p++, dao.getExecutorGroup());
        ps.setString(p++, "processing");
        ps.setInt(p++, crashedExecutorId);
        return ps;
      }}, keyHolder);
    int id = keyHolder.getKey().intValue();
    dao.recoverWorkflowInstancesFromDeadNodes();
    WorkflowInstance recoveredInstance = workflowInstanceDao.getWorkflowInstance(id);
    assertThat(recoveredInstance.executorId, is(nullValue()));
    assertThat(recoveredInstance.actions.size(), is(1));
    WorkflowInstanceAction workflowInstanceAction = recoveredInstance.actions.get(0);
    assertThat(workflowInstanceAction.executorId, is(currentExecutorId));
    assertThat(workflowInstanceAction.stateText, is("Recovered"));
  }
}
