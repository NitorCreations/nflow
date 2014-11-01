package com.nitorcreations.nflow.rest.v1.converter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.nitorcreations.nflow.engine.workflow.definition.StateExecutionStatistics;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;

@Component
public class WorkflowDefinitionStatisticsConverter {

  public WorkflowDefinitionStatisticsResponse convert(Map<String, StateExecutionStatistics> stats) {
    WorkflowDefinitionStatisticsResponse resp = new WorkflowDefinitionStatisticsResponse();
    for (Entry<String, StateExecutionStatistics> entry : stats.entrySet()) {
      StateExecutionStatistics stateStats = entry.getValue();
      Map<String, Long> values = new LinkedHashMap<>(4);
      values.put("executing", stateStats.executing);
      values.put("sleeping", stateStats.sleeping);
      values.put("queued", stateStats.queued);
      values.put("nonScheduled", stateStats.nonScheduled);
      resp.stateStatistics.put(entry.getKey(), values);
    }
    return resp;
  }
}
