package com.nitorcreations.nflow.engine.service;

import java.util.Map;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.dao.StatisticsDao;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;

/**
 * Service for fetching statistics.
 */
@Component
public class StatisticsService {

  @Inject
  private StatisticsDao statisticsDao;

  /**
   * Return queue statistics for the executor group.
   * @return Queue statistics.
   */
  public Statistics queryStatistics() {
    return statisticsDao.getQueueStatistics();
  }

  /**
   * Return statistics for a given workflow definition type.
   * @param type The workflow definition type.
   * @param createdAfter If given, count only workflow instances created after this time.
   * @param createdBefore If given, count only workflow instances created before this time.
   * @param modifiedAfter If given, count only workflow instances modified after this time.
   * @param modifiedBefore If given, count only workflow instances modified after this time.
   * @return The statistics per workflow state and status.
   */
  public Map<String, Map<String, WorkflowDefinitionStatistics>> getWorkflowDefinitionStatistics(String type,
      DateTime createdAfter, DateTime createdBefore, DateTime modifiedAfter, DateTime modifiedBefore) {
    return statisticsDao.getWorkflowDefinitionStatistics(type, createdAfter, createdBefore, modifiedAfter, modifiedBefore);
  }
}
