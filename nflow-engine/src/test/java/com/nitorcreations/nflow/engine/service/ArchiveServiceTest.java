package com.nitorcreations.nflow.engine.service;

import com.nitorcreations.nflow.engine.internal.dao.ArchiveDao;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveServiceTest {
  @InjectMocks
  private final ArchiveService service = new ArchiveService();
  @Mock
  private ArchiveDao dao;
  private DateTime limit = new DateTime(2015,7,10,19,57,0,0);
  private List<Integer> emptyList = Collections.emptyList();
  private List<Integer> dataList = Arrays.asList(1,2,3,4,5,6,7,8,9,10);

  @Test
  public void withZeroWorkflowsInFirstBatchCausesNothingToArchive() {
    when(dao.listArchivableWorkflows(limit, 10)).thenReturn(emptyList);
    int archived = service.archiveWorkflows(limit, 10);
    assertEquals(0, archived);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao).listArchivableWorkflows(limit, 10);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void archivingContinuesUntilEmptyListOfArchivableIsReturned() {
    when(dao.listArchivableWorkflows(limit, 10)).thenReturn(dataList, dataList, dataList, emptyList);
    int archived = service.archiveWorkflows(limit, 10);
    assertEquals(dataList.size() * 3, archived);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao, times(4)).listArchivableWorkflows(limit, 10);
    verify(dao, times(3)).archiveWorkflows(dataList);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void noArchivingHappensWhenValidArchiveTablesDoNotExist() {
    doThrow(new IllegalArgumentException("bad archive table")).when(dao).ensureValidArchiveTablesExist();
    try {
      service.archiveWorkflows(limit, 10);
      fail("exception expected");
    } catch(IllegalArgumentException e) {
      // ignore
    }
    verify(dao).ensureValidArchiveTablesExist();
    verifyNoMoreInteractions(dao);
  }
}
