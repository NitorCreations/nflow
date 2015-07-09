package com.nitorcreations.nflow.engine.internal.dao;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance.WorkflowInstanceStatus.created;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.junit.Test;
import org.springframework.dao.EmptyResultDataAccessException;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

public class ArchiveDaoTest extends BaseDaoTest {
  @Inject
  ArchiveDao archiveDao;
  @Inject
  WorkflowInstanceDao workflowInstanceDao;

  DateTime archiveTimeLimit = new DateTime(2015,7,8, 21,28,0,0);

  DateTime archiveTime1 = archiveTimeLimit.minus(1);
  DateTime archiveTime2 = archiveTimeLimit.minusMinutes(1);
  DateTime archiveTime3 = archiveTimeLimit.minusHours(2);
  DateTime archiveTime4 = archiveTimeLimit.minusDays(3);

  DateTime prodTime1 = archiveTimeLimit.plus(1);
  DateTime prodTime2 = archiveTimeLimit.plusMinutes(1);
  DateTime prodTime3 = archiveTimeLimit.plusHours(2);
  DateTime prodTime4 = archiveTimeLimit.plusDays(3);

  // TODO implement tests for actions, states
  // TODO implement tests for child workflows and their actions, states

  @Test
  public void listingArchivableWorkflows() {
    List<Integer> expectedArchive = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime2);

    expectedArchive.add(storePassiveWorkflow(archiveTime1));
    expectedArchive.add(storePassiveWorkflow(archiveTime2));

    List<Integer> archivableIds = archiveDao.listArchivableWorkflows(archiveTimeLimit, 10);
    assertEqualsInAnyOrder(expectedArchive, archivableIds);
  }

  @Test
  public void listingReturnsOldestRowsAndMaxBatchSizeRows() {
    List<Integer> expectedArchive = new ArrayList<>();

    int eleventh = storePassiveWorkflow(archiveTime2);

    for(int i = 0; i < 9; i++){
      expectedArchive.add(storePassiveWorkflow(archiveTime4));
    }
    expectedArchive.add(storePassiveWorkflow(archiveTime3));

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime3);
    storePassiveWorkflow(prodTime4);

    List<Integer> archivableIds = archiveDao.listArchivableWorkflows(archiveTimeLimit, 10);
    Collections.sort(archivableIds);
    assertEquals(expectedArchive, archivableIds);

    expectedArchive.add(eleventh);
    archivableIds = archiveDao.listArchivableWorkflows(archiveTimeLimit, 11);
    assertEqualsInAnyOrder(expectedArchive, archivableIds);
  }

  @Test
  public void archivingSimpleWorkflowsWorks() {
    List<Integer> expectedArchive = new ArrayList<>();

    storeActiveWorkflow(archiveTime1);
    storeActiveWorkflow(prodTime1);
    storePassiveWorkflow(prodTime1);

    expectedArchive.add(storePassiveWorkflow(archiveTime1));
    expectedArchive.add(storePassiveWorkflow(archiveTime2));

    archiveDao.archiveWorkflows(expectedArchive);

    assertActiveWorkflowsRemoved(expectedArchive);
    assertArchiveWorkflowsExists(expectedArchive);
  }

  private void assertActiveWorkflowsRemoved(List<Integer> workflowIds) {
    for(int id: workflowIds){
      try {
        workflowInstanceDao.getWorkflowInstance(id);
        fail("Expected workflow " + id + " to be removed");
      } catch(EmptyResultDataAccessException e) {
        // expected exception
      }
    }
  }

  private void assertArchiveWorkflowsExists(List<Integer> workflowIds) {
    for(int workflowId : workflowIds){
      Map<String, Object> archived = getArchivedWorkflow(workflowId);
      assertEquals(workflowId, archived.get("id"));
    }
  }

  // TODO re-implement using archive searches in daos when they are implement
  private Map<String, Object> getArchivedWorkflow(int workflowId) {
    return jdbc.queryForMap("select * from nflow_archive_workflow where id = ?", new Object[]{workflowId});
  }

  private int storePassiveWorkflow(DateTime modified) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setNextActivation(null).setModified(modified).build();
    int id = insert(instance);
    return id;
  }

  private int storeActiveWorkflow(DateTime modified) {
    WorkflowInstance instance = constructWorkflowInstanceBuilder().setStatus(created).setNextActivation(now()).setModified(modified).build();
    int id = insert(instance);
    return id;
  }

  private int insert(WorkflowInstance instance) {
    // TODO insertWorkflowInstance doesn't support storing modified date. Add some magic internal variable to enable that?
    int id = workflowInstanceDao.insertWorkflowInstance(instance);
    assertTrue(id > 0);
    DateTime modified = instance.modified;
    updateModified(id, modified);
    WorkflowInstance dbInstance = workflowInstanceDao.getWorkflowInstance(id);
    assertEquals(modified, dbInstance.modified);
    return id;
  }

  private void updateModified(int workflowId, DateTime modified) {
    int updateCount = jdbc.update("update nflow_workflow set modified = ? where id = ?",
            new Object[]{ DaoUtil.toTimestamp(modified), workflowId });
    assertEquals(1, updateCount);
  }

  private void assertEqualsInAnyOrder(List<Integer> expected, List<Integer> actual) {
    List<Integer> expectedCopy = new ArrayList<>(expected);
    List<Integer> actualCopy = new ArrayList<>(actual);
    Collections.sort(expectedCopy);
    Collections.sort(actualCopy);
    assertEquals(expectedCopy, actualCopy);
  }
}
