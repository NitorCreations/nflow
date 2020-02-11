package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.MaintenanceDao.TablePrefix.MAIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
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
import org.joda.time.Period;
import org.joda.time.ReadablePeriod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;

@ExtendWith(MockitoExtension.class)
public class MaintenanceServiceTest {

  private static final int BATCH_SIZE = 10;
  private MaintenanceService service;
  @Mock
  private MaintenanceDao dao;
  private final DateTime limit = new DateTime(2015, 7, 10, 19, 57, 0, 0);
  private final List<Long> emptyList = emptyList();
  private final List<Long> oldWorkdlowIds = asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
  private MaintenanceConfiguration archiveConfig;
  private MaintenanceConfiguration deleteMainConfig;
  private MaintenanceConfiguration deleteArchiveConfig;

  @BeforeEach
  public void setup() {
    service = new MaintenanceService(dao);
    setCurrentMillisFixed(currentTimeMillis());
    ReadablePeriod period = new Period(limit, now());
    ConfigurationItem configItem = new ConfigurationItem.Builder().setOlderThanPeriod(period).setBatchSize(BATCH_SIZE).build();
    archiveConfig = new MaintenanceConfiguration.Builder().setArchiveWorkflows(configItem).build();
    deleteMainConfig = new MaintenanceConfiguration.Builder().setDeleteWorkflows(configItem).build();
    deleteArchiveConfig = new MaintenanceConfiguration.Builder().setDeleteArchivedWorkflows(configItem).build();
  }

  @AfterEach
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  public void withZeroOldWorkflowsNothingIsArchived() {
    when(dao.listOldWorkflows(MAIN, limit, BATCH_SIZE)).thenReturn(emptyList);

    MaintenanceResults results = service.cleanupWorkflows(archiveConfig);

    assertEquals(0, results.archivedWorkflows);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao).listOldWorkflows(MAIN, limit, BATCH_SIZE);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void withZeroOldWorkflowsNothingIsDeleted() {
    when(dao.listOldWorkflows(MAIN, limit, BATCH_SIZE)).thenReturn(emptyList);

    MaintenanceResults results = service.cleanupWorkflows(deleteMainConfig);

    assertEquals(0, results.deletedWorkflows);
    verify(dao).listOldWorkflows(MAIN, limit, BATCH_SIZE);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void withZeroOldWorkflowsNothingIsDeletedFromArchiveTables() {
    when(dao.listOldWorkflows(ARCHIVE, limit, BATCH_SIZE)).thenReturn(emptyList);

    MaintenanceResults results = service.cleanupWorkflows(deleteArchiveConfig);

    assertEquals(0, results.deletedArchivedWorkflows);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao).listOldWorkflows(ARCHIVE, limit, BATCH_SIZE);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void archivingContinuesAsLongAsOldWorkflowsAreFound() {
    doReturn(oldWorkdlowIds, oldWorkdlowIds, oldWorkdlowIds, emptyList).when(dao).listOldWorkflows(MAIN, limit, BATCH_SIZE);
    when(dao.archiveWorkflows(oldWorkdlowIds)).thenReturn(oldWorkdlowIds.size());

    MaintenanceResults results = service.cleanupWorkflows(archiveConfig);

    assertEquals(oldWorkdlowIds.size() * 3, results.archivedWorkflows);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao, times(4)).listOldWorkflows(MAIN, limit, BATCH_SIZE);
    verify(dao, times(3)).archiveWorkflows(oldWorkdlowIds);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void deletingFromMainTablesContinuesAsLongAsOldWorkflowsAreFound() {
    doReturn(oldWorkdlowIds, oldWorkdlowIds, oldWorkdlowIds, emptyList).when(dao).listOldWorkflows(MAIN, limit, BATCH_SIZE);
    when(dao.deleteWorkflows(MAIN, oldWorkdlowIds)).thenReturn(oldWorkdlowIds.size());

    MaintenanceResults results = service.cleanupWorkflows(deleteMainConfig);

    assertEquals(oldWorkdlowIds.size() * 3, results.deletedWorkflows);
    verify(dao, times(4)).listOldWorkflows(MAIN, limit, BATCH_SIZE);
    verify(dao, times(3)).deleteWorkflows(MAIN, oldWorkdlowIds);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void deletingFromArchiveTablesContinuesAsLongAsOldWorkflowsAreFound() {
    doReturn(oldWorkdlowIds, oldWorkdlowIds, oldWorkdlowIds, emptyList).when(dao).listOldWorkflows(ARCHIVE, limit, BATCH_SIZE);
    when(dao.deleteWorkflows(ARCHIVE, oldWorkdlowIds)).thenReturn(oldWorkdlowIds.size());

    MaintenanceResults results = service.cleanupWorkflows(deleteArchiveConfig);

    assertEquals(oldWorkdlowIds.size() * 3, results.deletedArchivedWorkflows);
    verify(dao).ensureValidArchiveTablesExist();
    verify(dao, times(4)).listOldWorkflows(ARCHIVE, limit, BATCH_SIZE);
    verify(dao, times(3)).deleteWorkflows(ARCHIVE, oldWorkdlowIds);
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void nothingIsArchivedWhenValidArchiveTablesDoNotExist() {
    doThrow(new IllegalArgumentException("bad archive table")).when(dao).ensureValidArchiveTablesExist();
    assertThrows(IllegalArgumentException.class, () -> service.cleanupWorkflows(archiveConfig));
    verify(dao).ensureValidArchiveTablesExist();
    verifyNoMoreInteractions(dao);
  }

  @Test
  public void nothingIsDeletedFromArchiveTablesWhenValidArchiveTablesDoNotExist() {
    doThrow(new IllegalArgumentException("bad archive table")).when(dao).ensureValidArchiveTablesExist();
    assertThrows(IllegalArgumentException.class, () -> service.cleanupWorkflows(deleteArchiveConfig));
    verify(dao).ensureValidArchiveTablesExist();
    verifyNoMoreInteractions(dao);
  }

}
