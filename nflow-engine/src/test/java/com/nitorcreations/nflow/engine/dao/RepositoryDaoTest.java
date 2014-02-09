package com.nitorcreations.nflow.engine.dao;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import com.nitorcreations.nflow.engine.domain.WorkflowInstance;

public class RepositoryDaoTest extends BaseDaoTest {

  @Inject
  RepositoryDao dao;
  
  @Inject
  DataSource ds;
  
  @Test
  public void roundTripTest() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(i1);
    WorkflowInstance i2 = dao.getWorkflowInstance(id);
    assertThat(i2.id, notNullValue());
    assertThat(i2.created, notNullValue());
    assertThat(i2.modified, notNullValue());
    assertThat(i1.type, equalTo(i2.type));
    assertThat(i1.state, equalTo(i2.state));
    assertThat(i1.stateText, equalTo(i2.stateText));
    assertThat(i1.nextActivation, equalTo(i2.nextActivation));
    assertThat(i1.processing, equalTo(i2.processing));
    assertThat(i1.requestData, equalTo(i2.requestData));
  }
  
  @Test
  public void updateWorkflowInstance() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().build();
    int id = dao.insertWorkflowInstance(i1);
    final WorkflowInstance i2 = new WorkflowInstance.Builder(dao.getWorkflowInstance(id))
      .setState("updateState")
      .setStateText("update text")
      .setNextActivation(DateTime.now())
      .setProcessing(!i1.processing)
      .build();
    dao.updateWorkflowInstance(i2);
    JdbcTemplate template = new JdbcTemplate(ds);
    template.query("select * from nflow_workflow where id = " + id, new RowCallbackHandler() {
      @Override
      public void processRow(ResultSet rs) throws SQLException {
        assertThat(rs.getString("state"), equalTo(i2.state));
        assertThat(rs.getString("state_text"), equalTo(i2.stateText));
        assertThat(rs.getTimestamp("next_activation").getTime(), equalTo(i2.nextActivation.toDate().getTime()));
        assertThat(rs.getBoolean("is_processing"), equalTo(i2.processing));
      }
    });
  }
  
  @Test
  public void pollNextWorkflowInstances() {
    WorkflowInstance i1 = constructWorkflowInstanceBuilder().setNextActivation(DateTime.now().minusMinutes(1)).setOwner("junit").build();
    int id = dao.insertWorkflowInstance(i1);
    List<Integer> firstBatch = dao.pollNextWorkflowInstanceIds(100);
    List<Integer> secondBatch = dao.pollNextWorkflowInstanceIds(100);    
    assertThat(firstBatch.size(), equalTo(1));
    assertThat(firstBatch.get(0), equalTo(id));
    assertThat(secondBatch.size(), equalTo(0));
  }
 
  @Test
  public void pollNextWorkflowInstancesWithRaceCondition() throws InterruptedException {
    for (int i=0; i<10000; i++) {
      WorkflowInstance instance = constructWorkflowInstanceBuilder().setNextActivation(DateTime.now().minusMinutes(1)).setOwner("junit").build();
      dao.insertWorkflowInstance(instance);
    }
    Poller[] pollers = new Poller[] { new Poller(dao), new Poller(dao) };
    Thread[] threads = new Thread[] { new Thread(pollers[0]), new Thread(pollers[1]) };
    threads[0].start();
    threads[1].start();
    threads[0].join();
    threads[1].join();
    assertTrue(pollers[0].detectedRaceCondition || pollers[1].detectedRaceCondition);
  }
  
  static class Poller implements Runnable {
    
    RepositoryDao dao;
    boolean detectedRaceCondition = false;
    
    public Poller(RepositoryDao dao) {
      this.dao = dao;
    }

    @Override
    public void run() {
      try {
        dao.pollNextWorkflowInstanceIds(10000);
      } catch(Exception ex) {
        ex.printStackTrace();
        detectedRaceCondition = ex.getMessage().startsWith("Race condition");
      }
    }
        
  }
  
  
}
