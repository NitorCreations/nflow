package com.nitorcreations.nflow.engine.internal.dao;

import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.nitorcreations.nflow.engine.workflow.definition.StateExecutionStatistics;
import com.nitorcreations.nflow.engine.workflow.instance.QueryWorkflowInstances;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;
import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction;

public class WorkflowInstanceDaoTest extends BaseDaoTest {

  @Inject
  WorkflowInstanceDao dao;

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
    int id = dao.insertWorkflowInstance(i1);
    assertThat(id, not(equalTo(-1)));
    QueryWorkflowInstances q = new QueryWorkflowInstances.Builder().addIds(id).addTypes(i1.type).addStates(i1.state)
        .setBusinessKey(i1.businessKey).setExternalId(i1.externalId).setIncludeActions(true)
        .setIncludeActionStateVariables(true).setIncludeCurrentStateVariables(true).build();
    List<WorkflowInstance> l = dao.queryWorkflowInstances(q);
    assertThat(l.size(), is(1));
    checkSameWorkflowInfo(i1, l.get(0));
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
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(i1);
    final WorkflowInstance i2 = new WorkflowInstance.Builder(dao.getWorkflowInstance(id)).setState("updateState")
        .setStateText("update text").setNextActivation(DateTime.now()).setProcessing(!i1.processing).build();
    final DateTime originalModifiedTime = dao.getWorkflowInstance(id).modified;
    sleep(1);
    dao.updateWorkflowInstance(i2);
    JdbcTemplate template = new JdbcTemplate(ds);
    template.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("state"), equalTo(i2.state));
        assertThat(rs.getString("state_text"), equalTo(i2.stateText));
        assertThat(rs.getTimestamp("next_activation").getTime(), equalTo(i2.nextActivation.toDate().getTime()));
        assertThat(rs.getInt("executor_id") != 0, equalTo(i2.processing));
        assertThat(rs.getTimestamp("modified").getTime(), greaterThan(originalModifiedTime.getMillis()));
      }
    });
  }

  @Test
  public void insertWorkflowInstanceActionWorks() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    i1.stateVariables.put("a", "1");
    int id = dao.insertWorkflowInstance(i1);
    DateTime started = DateTime.now();
    WorkflowInstanceAction a1 = new WorkflowInstanceAction.Builder().setExecutionStart(started).setExecutorId(42)
        .setExecutionEnd(DateTime.now().plusMillis(100)).setRetryNo(1).setState("test").setStateText("state text")
        .setWorkflowInstanceId(id).build();
    i1.stateVariables.put("b", "2");
    dao.insertWorkflowInstanceAction(i1, a1);
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
  public void pollNextWorkflowInstancesWithRaceCondition() throws InterruptedException {
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
    assertTrue("Race condition should happen", pollers[0].detectedRaceCondition || pollers[1].detectedRaceCondition);
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
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setExecutorGroup("junit").setNextActivation(null).build();
    int id = dao.insertWorkflowInstance(i1);
    dao.updateWorkflowInstance(new WorkflowInstance.Builder(i1).setId(id).setProcessing(true).build());
    assertThat(dao.getWorkflowInstance(id).nextActivation, nullValue());
    dao.wakeupWorkflowInstanceIfNotExecuting(id, new String[] { i1.state });
    assertThat(dao.getWorkflowInstance(id).nextActivation, nullValue());
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
  public void getStatisticsWorks() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    int id = dao.insertWorkflowInstance(i1);

    Map<String, StateExecutionStatistics> statsMap = dao.getStateExecutionStatistics(i1.type, null, null, null, null);
    StateExecutionStatistics stats = statsMap.get("CreateLoan");
    assertThat(stats.executing, is(0L));
    assertThat(stats.queued, is(0L));
    assertThat(stats.sleeping, is(0L));
    assertThat(stats.nonScheduled, is(1L));

    dao.wakeupWorkflowInstanceIfNotExecuting(id, new String[] { "otherState", i1.state });

    statsMap = dao.getStateExecutionStatistics(i1.type, null, null, null, null);
    stats = statsMap.get("CreateLoan");
    assertThat(stats.executing, is(0L));
    assertThat(stats.queued, is(1L));
    assertThat(stats.sleeping, is(0L));
    assertThat(stats.nonScheduled, is(0L));
  }

  @Test
  public void getStatisticsWorksWithCreatedLimits() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    dao.insertWorkflowInstance(i1);

    Map<String, StateExecutionStatistics> statsMap = dao.getStateExecutionStatistics(i1.type, now().plusDays(1), now().plusDays(2), null, null);

    assertThat(statsMap.size(), is(0));
  }

  @Test
  public void getStatisticsWorksWithModifiedLimits() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    dao.insertWorkflowInstance(i1);

    Map<String, StateExecutionStatistics> statsMap = dao.getStateExecutionStatistics(i1.type, null, null, now().plusDays(1), now().plusDays(2));

    assertThat(statsMap.size(), is(0));
  }

  @Test
  public void getStatisticsWorksWithCreatedAndModifiedLimits() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(null).build();
    dao.insertWorkflowInstance(i1);

    Map<String, StateExecutionStatistics> statsMap = dao.getStateExecutionStatistics(i1.type, now().minusDays(1), now().plusDays(1), now().minusDays(1), now().plusDays(1));

    assertThat(statsMap.size(), is(1));
  }

  private static void checkSameWorkflowInfo(WorkflowInstance i1, WorkflowInstance i2) {
    assertThat(i1.type, equalTo(i2.type));
    assertThat(i1.executorId, equalTo(i2.executorId));
    assertThat(i1.state, equalTo(i2.state));
    assertThat(i1.stateText, equalTo(i2.stateText));
    assertThat(i1.nextActivation, equalTo(i2.nextActivation));
    assertThat(i1.processing, equalTo(i2.processing));
    assertThat(i1.stateVariables.size(), equalTo(i2.stateVariables.size()));
    Map<String, String> tmpVars = new LinkedHashMap<>(i1.stateVariables);
    for (Map.Entry<String, String> entry : tmpVars.entrySet()) {
      assertTrue(i2.stateVariables.containsKey(entry.getKey()));
      assertThat(i2.stateVariables.get(entry.getKey()), equalTo(entry.getValue()));
    }
  }

  static class Poller implements Runnable {
    WorkflowInstanceDao dao;
    boolean detectedRaceCondition = false;
    int batchSize;

    public Poller(WorkflowInstanceDao dao, int batchSize) {
      this.dao = dao;
      this.batchSize = batchSize;
    }

    @Override
    public void run() {
      try {
        dao.pollNextWorkflowInstanceIds(batchSize);
      } catch (PollingRaceConditionException ex) {
        ex.printStackTrace();
        detectedRaceCondition = ex.getMessage().startsWith("Race condition");
      }
    }
  }

}
