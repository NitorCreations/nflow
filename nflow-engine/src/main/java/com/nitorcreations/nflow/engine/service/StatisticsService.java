package com.nitorcreations.nflow.engine.service;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.internal.dao.StatisticsDao;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;

/**
 * Service for fetching statistics.
 */
@Component
public class StatisticsService {

  private final StatisticsDao statisticsDao;

  @Inject
  public StatisticsService(StatisticsDao statisticsDao) {
    this.statisticsDao = statisticsDao;
  }

  public Statistics queryStatistics() {
    return statisticsDao.getQueueStatistics();
  }
}
