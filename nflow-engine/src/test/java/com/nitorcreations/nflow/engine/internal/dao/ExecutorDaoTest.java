package com.nitorcreations.nflow.engine.internal.dao;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.nitorcreations.nflow.engine.internal.dao.WorkflowInstanceDao.WorkflowInstanceActionRowMapper;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class ExecutorDaoTest extends BaseDaoTest {
  @Inject
  ExecutorDao dao;

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
    JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
    int crashedExecutorId = 999;
    insertCrashedExecutor(crashedExecutorId, jdbcTemplate);
    int id = insertWorkflowInstance(crashedExecutorId, jdbcTemplate);

    dao.recoverWorkflowInstancesFromDeadNodes();

    Integer executorId = jdbcTemplate.queryForObject("select executor_id from nflow_workflow where id = ?", Integer.class, id);
    assertThat(executorId, is(nullValue()));

    List<WorkflowInstanceAction> actions = jdbcTemplate.query("select * from nflow_workflow_action where workflow_id = ?",
        new WorkflowInstanceActionRowMapper(), id);
    assertThat(actions.size(), is(1));
    WorkflowInstanceAction workflowInstanceAction = actions.get(0);
    assertThat(workflowInstanceAction.executorId, is(dao.getExecutorId()));
    assertThat(workflowInstanceAction.stateText, is("Recovered"));
  }

  private int insertWorkflowInstance(final int crashedExecutorId, JdbcTemplate jdbcTemplate) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(new PreparedStatementCreator() {
      @Override
      public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
            "insert into nflow_workflow (type, external_id, executor_group, state, executor_id) values (?, ?, ?, ?, ?)",
            new String[] { "id" });
        ps.setString(1, "test");
        ps.setString(2, "extId");
        ps.setString(3, dao.getExecutorGroup());
        ps.setString(4, "processing");
        ps.setInt(5, crashedExecutorId);
        return ps;
      }
    }, keyHolder);
    return keyHolder.getKey().intValue();
  }

  private void insertCrashedExecutor(int crashedExecutorId, JdbcTemplate jdbcTemplate) {
    jdbcTemplate.update(
        "insert into nflow_executor (id, host, pid, executor_group, started, active, expires) values (?, ?, ?, ?, ?, ?, ?)",
        crashedExecutorId, "localhost", 666, dao.getExecutorGroup(), new Timestamp(now().minusDays(1).getMillis()),
        new Timestamp(now().minusDays(1).getMillis()), new Timestamp(now().minusHours(1).getMillis()));
  }
}
