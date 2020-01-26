package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.ArchiveDao.TablePrefix.MAIN;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.nflow.engine.internal.dao.ArchiveDao.TablePrefix;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.internal.dao.ArchiveDao;

@ExtendWith(MockitoExtension.class)
public class ArchiveServiceTest {

  private ArchiveService service;
  @Mock
  private ArchiveDao dao;
  private final DateTime limit = new DateTime(2015, 7, 10, 19, 57, 0, 0);
  private final List<Long> emptyList = Collections.emptyList();
  private final List<Long> dataList = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);

  @BeforeEach
  public void setup() {
    service = new ArchiveService(dao);
    setCurrentMillisFixed(currentTimeMillis());
  }

  @AfterEach
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  @SuppressWarnings("deprecation")
  public void withZeroWorkflowsInFirstBatchCausesNothingToArchive() {
    when(dao.listOldWorkflowTrees(MAIN, limit, 10)).thenReturn(emptyList);
    int archived = service.archiveWorkflows(limit, 10);
    assertEquals(0, archived);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao).listOldWorkflowTrees(MAIN, limit, 10);
    verifyNoMoreInteractions(dao);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void archivingContinuesUntilEmptyListOfArchivableIsReturned() {
    doReturn(dataList).doReturn(dataList).doReturn(dataList).doReturn(emptyList).when(dao).listOldWorkflowTrees(MAIN, limit, 10);
    when(dao.archiveWorkflows(dataList)).thenReturn(dataList.size());
    int archived = service.archiveWorkflows(limit, 10);
    assertEquals(dataList.size() * 3, archived);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao, times(4)).listOldWorkflowTrees(MAIN, limit, 10);
    verify(dao, times(3)).archiveWorkflows(dataList);
    verifyNoMoreInteractions(dao);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void noArchivingHappensWhenValidArchiveTablesDoNotExist() {
    doThrow(new IllegalArgumentException("bad archive table")).when(dao).ensureValidArchiveTablesExist();
    try {
      service.archiveWorkflows(limit, 10);
      fail("exception expected");
    } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
      // ignore
    }
    verify(dao).ensureValidArchiveTablesExist();
    verifyNoMoreInteractions(dao);
  }
}
