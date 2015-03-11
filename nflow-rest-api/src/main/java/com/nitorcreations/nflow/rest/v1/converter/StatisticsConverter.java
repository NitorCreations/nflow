package com.nitorcreations.nflow.rest.v1.converter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.rest.v1.msg.DefinitionStatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;

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

  public WorkflowDefinitionStatisticsResponse convert(Map<String, Map<String, WorkflowDefinitionStatistics>> stats) {
    WorkflowDefinitionStatisticsResponse resp = new WorkflowDefinitionStatisticsResponse();
    for (Entry<String, Map<String, WorkflowDefinitionStatistics>> entry : stats.entrySet()) {
      LinkedHashMap<String, DefinitionStatisticsResponse> statusStats = new LinkedHashMap<>();
      resp.stateStatistics.put(entry.getKey(), statusStats);
      for (Entry<String, WorkflowDefinitionStatistics> statusEntry : entry.getValue().entrySet()) {
        DefinitionStatisticsResponse statsResponse = new DefinitionStatisticsResponse();
        WorkflowDefinitionStatistics statistics = statusEntry.getValue();
        statsResponse.allInstances = statistics.allInstances;
        statsResponse.queuedInstances = statistics.queuedInstances;
        statusStats.put(statusEntry.getKey(), statsResponse);
      }
    }
    return resp;
  }
}
