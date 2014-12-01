package com.nitorcreations.nflow.rest.v1.converter;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;

@Component
public class StatisticsConverter {

  public StatisticsResponse convert(Statistics stats) {
    StatisticsResponse response = new StatisticsResponse();
    response.queueStatistics.count = stats.queuedStatistics.count;
    response.queueStatistics.maxAge = stats.queuedStatistics.maxAgeMillis;
    response.queueStatistics.minAge = stats.queuedStatistics.minAgeMillis;

    response.executionStatistics.count = stats.executionStatistics.count;
    response.executionStatistics.maxAge = stats.executionStatistics.maxAgeMillis;
    response.executionStatistics.minAge = stats.executionStatistics.minAgeMillis;
    return response;
  }
}
