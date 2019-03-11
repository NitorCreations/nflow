package io.nflow.engine.service;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.verify;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import io.nflow.engine.internal.dao.StatisticsDao;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {

  @InjectMocks
  private final StatisticsService service = new StatisticsService();
  @Mock
  private StatisticsDao dao;

  DateTime createdAfter = now(), createdBefore = now().plusMinutes(1), modifiedAfter = now().plusMinutes(2),
          modifiedBefore = now().plusMinutes(3);

  @Test
  public void queryStatisticsDelegatesToDao() {
    service.getStatistics();

    verify(dao).getQueueStatistics();
  }

  @Test
  public void getWorkflowDefinitionStatisticsDelegatesToDao() {
    service.getWorkflowDefinitionStatistics("type", createdAfter, createdBefore, modifiedAfter, modifiedBefore);

    verify(dao).getWorkflowDefinitionStatistics("type", createdAfter, createdBefore, modifiedAfter, modifiedBefore);
  }
}
