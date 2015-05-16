package com.nitorcreations.nflow.engine.service;

import static org.joda.time.DateTime.now;
import static org.mockito.Mockito.verify;

import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.nitorcreations.nflow.engine.internal.dao.StatisticsDao;

@RunWith(MockitoJUnitRunner.class)
public class StatisticsServiceTest {

  @InjectMocks
  private final StatisticsService service = new StatisticsService();
  @Mock
  private StatisticsDao dao;

  DateTime createdAfter = now(), createdBefore = now().plusMinutes(1), modifiedAfter = now().plusMinutes(2),
          modifiedBefore = now().plusMinutes(3);

  @Test
  public void queryStatisticsDelegatesToDao() {
    service.queryStatistics();

    verify(dao).getQueueStatistics();
  }

  @Test
  public void getWorkflowDefinitionStatisticsDelegatesToDao() {
    service.getWorkflowDefinitionStatistics("type", createdAfter, createdBefore, modifiedAfter, modifiedBefore);

    verify(dao).getWorkflowDefinitionStatistics("type", createdAfter, createdBefore, modifiedAfter, modifiedBefore);
  }
}
