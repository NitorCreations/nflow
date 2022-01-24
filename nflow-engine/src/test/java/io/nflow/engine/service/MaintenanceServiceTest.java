package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.TableType.ARCHIVE;
import static io.nflow.engine.internal.dao.TableType.MAIN;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
import static org.joda.time.Period.months;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.ReadablePeriod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.NflowTable;
import io.nflow.engine.internal.dao.TableMetadataChecker;

@ExtendWith(MockitoExtension.class)
public class MaintenanceServiceTest {

  private static final int BATCH_SIZE = 10;
  private MaintenanceService service;
  @Mock
  private MaintenanceDao dao;
  @Mock
  private TableMetadataChecker tableMetadataChecker;
  @Mock
  private WorkflowDefinitionService workflowDefinitionService;
  private final List<Long> emptyList = emptyList();
  private final List<Long> oldWorkdlowIds = asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
  private MaintenanceConfiguration archiveConfig;
  private MaintenanceConfiguration deleteMainConfig;
  private MaintenanceConfiguration deleteArchiveConfig;
  private ReadablePeriod period;
  private DateTime limit;

  @BeforeEach
  public void setup() {
    service = new MaintenanceService(dao, tableMetadataChecker, workflowDefinitionService);
    setCurrentMillisFixed(currentTimeMillis());
    period = months(1);
    limit = now().minus(period);
    archiveConfig = new MaintenanceConfiguration.Builder().withArchiveWorkflows().setOlderThanPeriod(period).setBatchSize(BATCH_SIZE).done().build();
    deleteMainConfig = new MaintenanceConfiguration.Builder().withDeleteWorkflows().setOlderThanPeriod(period).setBatchSize(BATCH_SIZE).done().build();
    deleteArchiveConfig = new MaintenanceConfiguration.Builder().withDeleteArchivedWorkflows().setOlderThanPeriod(period).setBatchSize(BATCH_SIZE).done().build();
  }

  @AfterEach
  public void reset() {
    setCurrentMillisSystem();
  }

  @Test
  public void withZeroOldWorkflowsNothingIsArchived() {
    when(dao.getOldWorkflowIds(MAIN, limit, BATCH_SIZE, emptySet())).thenReturn(emptyList);

    MaintenanceResults results = service.cleanupWorkflows(archiveConfig);

    assertEquals(0, results.archivedWorkflows);
    assertValidArchiveTablesAreChecked();
    verify(dao).getOldWorkflowIds(MAIN, limit, BATCH_SIZE, emptySet());
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

  private void assertValidArchiveTablesAreChecked() {
    stream(NflowTable.values()).forEach(table -> verify(tableMetadataChecker).ensureCopyingPossible(table.main, table.archive));
  }

  @Test
  public void withZeroOldWorkflowsNothingIsDeleted() {
    when(dao.getOldWorkflowIds(MAIN, limit, BATCH_SIZE, emptySet())).thenReturn(emptyList);

    MaintenanceResults results = service.cleanupWorkflows(deleteMainConfig);

    assertEquals(0, results.deletedWorkflows);
    verify(dao).getOldWorkflowIds(MAIN, limit, BATCH_SIZE, emptySet());
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

  @Test
  public void withZeroOldWorkflowsNothingIsDeletedFromArchiveTables() {
    when(dao.getOldWorkflowIds(ARCHIVE, limit, BATCH_SIZE, emptySet())).thenReturn(emptyList);

    MaintenanceResults results = service.cleanupWorkflows(deleteArchiveConfig);

    assertEquals(0, results.deletedArchivedWorkflows);
    assertValidArchiveTablesAreChecked();
    verify(dao).getOldWorkflowIds(ARCHIVE, limit, BATCH_SIZE, emptySet());
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

  @Test
  public void archivingContinuesAsLongAsOldWorkflowsAreFound() {
    doReturn(oldWorkdlowIds, oldWorkdlowIds, oldWorkdlowIds, emptyList).when(dao).getOldWorkflowIds(MAIN, limit, BATCH_SIZE,
        emptySet());
    when(dao.archiveWorkflows(oldWorkdlowIds)).thenReturn(oldWorkdlowIds.size());

    MaintenanceResults results = service.cleanupWorkflows(archiveConfig);

    assertEquals(oldWorkdlowIds.size() * 3, results.archivedWorkflows);
    assertValidArchiveTablesAreChecked();
    verify(dao, times(4)).getOldWorkflowIds(MAIN, limit, BATCH_SIZE, emptySet());
    verify(dao, times(3)).archiveWorkflows(oldWorkdlowIds);
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

  @Test
  public void deletingFromMainTablesContinuesAsLongAsOldWorkflowsAreFound() {
    doReturn(oldWorkdlowIds, oldWorkdlowIds, oldWorkdlowIds, emptyList).when(dao).getOldWorkflowIds(MAIN, limit, BATCH_SIZE,
        emptySet());
    when(dao.deleteWorkflows(MAIN, oldWorkdlowIds)).thenReturn(oldWorkdlowIds.size());

    MaintenanceResults results = service.cleanupWorkflows(deleteMainConfig);

    assertEquals(oldWorkdlowIds.size() * 3, results.deletedWorkflows);
    verify(dao, times(4)).getOldWorkflowIds(MAIN, limit, BATCH_SIZE, emptySet());
    verify(dao, times(3)).deleteWorkflows(MAIN, oldWorkdlowIds);
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

  @Test
  public void deletingFromArchiveTablesContinuesAsLongAsOldWorkflowsAreFound() {
    doReturn(oldWorkdlowIds, oldWorkdlowIds, oldWorkdlowIds, emptyList).when(dao).getOldWorkflowIds(ARCHIVE, limit, BATCH_SIZE,
        emptySet());
    when(dao.deleteWorkflows(ARCHIVE, oldWorkdlowIds)).thenReturn(oldWorkdlowIds.size());

    MaintenanceResults results = service.cleanupWorkflows(deleteArchiveConfig);

    assertEquals(oldWorkdlowIds.size() * 3, results.deletedArchivedWorkflows);
    assertValidArchiveTablesAreChecked();
    verify(dao, times(4)).getOldWorkflowIds(ARCHIVE, limit, BATCH_SIZE, emptySet());
    verify(dao, times(3)).deleteWorkflows(ARCHIVE, oldWorkdlowIds);
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

  @Test
  public void nothingIsArchivedWhenValidArchiveTablesDoNotExist() {
    doThrow(IllegalArgumentException.class).when(tableMetadataChecker).ensureCopyingPossible(anyString(), anyString());
    assertThrows(IllegalArgumentException.class, () -> service.cleanupWorkflows(archiveConfig));
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

  @Test
  public void nothingIsDeletedFromArchiveTablesWhenValidArchiveTablesDoNotExist() {
    doThrow(IllegalArgumentException.class).when(tableMetadataChecker).ensureCopyingPossible(anyString(), anyString());
    assertThrows(IllegalArgumentException.class, () -> service.cleanupWorkflows(deleteArchiveConfig));
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

}
