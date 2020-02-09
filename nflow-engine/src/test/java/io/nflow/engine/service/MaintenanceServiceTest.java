package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix.MAIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.joda.time.Duration.millis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.service.MaintenanceService.MaintenanceConfiguration;
import io.nflow.engine.service.MaintenanceService.MaintenanceResults;

@ExtendWith(MockitoExtension.class)
public class MaintenanceServiceTest {

  private MaintenanceService service;
  @Mock
  private MaintenanceDao dao;
  private final DateTime limit = new DateTime(2015, 7, 10, 19, 57, 0, 0);
  private final List<Long> emptyList = emptyList();
  private final List<Long> dataList = asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
  private MaintenanceConfiguration config;

  @BeforeEach
  public void setup() {
    service = new MaintenanceService(dao);
    setCurrentMillisFixed(currentTimeMillis());
    Duration duration = millis(now().getMillis() - limit.getMillis());
    config = new MaintenanceConfiguration.Builder().setArchiveWorkflowsOlderThan(duration).setBatchSize(10).build();
  }

  @AfterEach
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  public void withZeroWorkflowsInFirstBatchCausesNothingToArchive() {
    when(dao.listOldWorkflows(MAIN, limit, 10)).thenReturn(emptyList);
    MaintenanceResults results = service.cleanupWorkflows(config);
    assertEquals(0, results.archivedWorkflows);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao).listOldWorkflows(MAIN, limit, 10);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void archivingContinuesUntilEmptyListOfArchivableIsReturned() {
    doReturn(dataList).doReturn(dataList).doReturn(dataList).doReturn(emptyList).when(dao).listOldWorkflows(MAIN, limit, 10);
    when(dao.archiveWorkflows(dataList)).thenReturn(dataList.size());
    MaintenanceResults results = service.cleanupWorkflows(config);
    assertEquals(dataList.size() * 3, results.archivedWorkflows);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao, times(4)).listOldWorkflows(MAIN, limit, 10);
    verify(dao, times(3)).archiveWorkflows(dataList);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void noArchivingHappensWhenValidArchiveTablesDoNotExist() {
    doThrow(new IllegalArgumentException("bad archive table")).when(dao).ensureValidArchiveTablesExist();
    assertThrows(IllegalArgumentException.class, () -> service.cleanupWorkflows(config));
    verify(dao).ensureValidArchiveTablesExist();
    verifyNoMoreInteractions(dao);
  }
}
