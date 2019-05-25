package io.nflow.engine.service;

import io.nflow.engine.internal.dao.StatisticsDao;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StatisticsServiceTest {

  private StatisticsService service;
  @Mock
  private StatisticsDao dao;
  private DateTime createdAfter = now(),
          createdBefore = now().plusMinutes(1),
          modifiedAfter = now().plusMinutes(2),
          modifiedBefore = now().plusMinutes(3);

  @BeforeEach
  public void setup() {
    service = new StatisticsService(dao);
  }

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
