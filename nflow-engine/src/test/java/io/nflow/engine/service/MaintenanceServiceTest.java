package io.nflow.engine.service;

import static io.nflow.engine.internal.dao.TablePrefix.ARCHIVE;
import static io.nflow.engine.internal.dao.TablePrefix.MAIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.joda.time.DateTime.now;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;
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
import java.util.stream.Stream;

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
import io.nflow.engine.internal.dao.TableMetadataChecker;
import io.nflow.engine.service.MaintenanceConfiguration.ConfigurationItem;

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
  private final DateTime limit = new DateTime(2015, 7, 10, 19, 57, 0, 0);
  private final List<Long> emptyList = emptyList();
  private final List<Long> oldWorkdlowIds = asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
  private MaintenanceConfiguration archiveConfig;
  private MaintenanceConfiguration deleteMainConfig;
  private MaintenanceConfiguration deleteArchiveConfig;

  @BeforeEach
  public void setup() {
    service = new MaintenanceService(dao, tableMetadataChecker, workflowDefinitionService);
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
    when(dao.getOldWorkflowIds(MAIN, limit, BATCH_SIZE, emptySet())).thenReturn(emptyList);

    MaintenanceResults results = service.cleanupWorkflows(archiveConfig);

    assertEquals(0, results.archivedWorkflows);
    assertValidArchiveTablesAreChecked();
    verify(dao).getOldWorkflowIds(MAIN, limit, BATCH_SIZE, emptySet());
    verifyNoMoreInteractions(dao, tableMetadataChecker);
  }

  private void assertValidArchiveTablesAreChecked() {
    Stream.of("workflow", "workflow_action", "workflow_state")
        .forEach(table -> verify(tableMetadataChecker).ensureCopyingPossible(MAIN.nameOf(table), ARCHIVE.nameOf(table)));
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
