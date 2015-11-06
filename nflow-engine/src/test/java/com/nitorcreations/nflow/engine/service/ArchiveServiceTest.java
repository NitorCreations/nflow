package com.nitorcreations.nflow.engine.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.internal.dao.ArchiveDao;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveServiceTest {
  @InjectMocks
  private final ArchiveService service = new ArchiveService();
  @Mock
  private ArchiveDao dao;
  private final DateTime limit = new DateTime(2015, 7, 10, 19, 57, 0, 0);
  private final List<Integer> emptyList = Collections.emptyList();
  private final List<Integer> dataList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

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
    doReturn(dataList).doReturn(dataList).doReturn(dataList).doReturn(emptyList).when(dao).listArchivableWorkflows(limit, 10);
    when(dao.archiveWorkflows(dataList)).thenReturn(dataList.size());
    when(dao.archiveWorkflows(emptyList)).thenReturn(emptyList.size());
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
    } catch (IllegalArgumentException e) {
      // ignore
    }
    verify(dao).ensureValidArchiveTablesExist();
    verifyNoMoreInteractions(dao);
  }
}
