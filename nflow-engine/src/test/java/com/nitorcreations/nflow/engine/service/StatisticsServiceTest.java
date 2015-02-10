package com.nitorcreations.nflow.engine.service;

import static org.mockito.Mockito.verify;

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

  @Test
  public void workflowDefinitionStatisticsWorks() {
    service.getWorkflowDefinitionStatistics("type", null, null, null, null);

    verify(dao).getWorkflowDefinitionStatistics("type", null, null, null, null);
  }
}
