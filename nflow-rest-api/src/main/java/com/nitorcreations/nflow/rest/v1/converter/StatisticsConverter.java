package com.nitorcreations.nflow.rest.v1.converter;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinitionStatistics;
import com.nitorcreations.nflow.engine.workflow.statistics.Statistics;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse.StateStatistics;

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
      StateStatistics stateStats = new StateStatistics();
      resp.stateStatistics.put(entry.getKey(), stateStats);
      for (Entry<String, WorkflowDefinitionStatistics> statusEntry : entry.getValue().entrySet()) {
        WorkflowDefinitionStatistics value = statusEntry.getValue();
        switch (statusEntry.getKey()) {
        case "created":
          stateStats.created.allInstances = value.allInstances;
          stateStats.created.queuedInstances = value.queuedInstances;
          break;
        case "inProgress":
          stateStats.inProgress.allInstances = value.allInstances;
          stateStats.inProgress.queuedInstances = value.queuedInstances;
          break;
        case "executing":
          stateStats.executing.allInstances = value.allInstances;
          break;
        case "manual":
          stateStats.manual.allInstances = value.allInstances;
          break;
        case "finished":
          stateStats.finished.allInstances = value.allInstances;
          break;
        default:
          // ignored
        }
      }
    }
    return resp;
  }
}
