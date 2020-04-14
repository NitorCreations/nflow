package io.nflow.engine.internal.executor;

import static org.joda.time.DateTime.now;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.nflow.engine.internal.dao.MaintenanceDao;
import io.nflow.engine.internal.dao.WorkflowInstanceDao;
import io.nflow.engine.internal.workflow.ObjectStringMapper;
import io.nflow.engine.internal.workflow.WorkflowInstancePreProcessor;
import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.service.WorkflowDefinitionService;
import io.nflow.engine.service.WorkflowInstanceService;

@Component
public class WorkflowStateProcessorFactory {
  private final WorkflowDefinitionService workflowDefinitions;
  private final WorkflowInstanceService workflowInstances;
  private final ObjectStringMapper objectMapper;
  private final WorkflowInstanceDao workflowInstanceDao;
  private final MaintenanceDao maintenanceDao;
  private final WorkflowInstancePreProcessor workflowInstancePreProcessor;
  private final Environment env;
  @Autowired(required = false)
  protected WorkflowExecutorListener[] listeners = new WorkflowExecutorListener[0];
  final Map<Long, WorkflowStateProcessor> processingInstances = new ConcurrentHashMap<>();
  private final int stuckThreadThresholdSeconds;

  @Inject
  public WorkflowStateProcessorFactory(WorkflowDefinitionService workflowDefinitions, WorkflowInstanceService workflowInstances,
      ObjectStringMapper objectMapper, WorkflowInstanceDao workflowInstanceDao, MaintenanceDao maintenanceDao,
      WorkflowInstancePreProcessor workflowInstancePreProcessor, Environment env) {
    this.workflowDefinitions = workflowDefinitions;
    this.workflowInstances = workflowInstances;
    this.objectMapper = objectMapper;
    this.workflowInstanceDao = workflowInstanceDao;
    this.maintenanceDao = maintenanceDao;
    this.workflowInstancePreProcessor = workflowInstancePreProcessor;
    this.stuckThreadThresholdSeconds = env.getRequiredProperty("nflow.executor.stuckThreadThreshold.seconds", Integer.class);
    this.env = env;
  }

  public WorkflowStateProcessor createProcessor(long instanceId, Supplier<Boolean> shutdownRequested) {
    return new WorkflowStateProcessor(instanceId, shutdownRequested, objectMapper, workflowDefinitions, workflowInstances, workflowInstanceDao,
        maintenanceDao, workflowInstancePreProcessor, env, processingInstances, listeners);
  }

  public int getPotentiallyStuckProcessors() {
    DateTime currentTime = now();
    int potentiallyStuck = 0;
    for (WorkflowStateProcessor processor : processingInstances.values()) {
      Duration processingTime = new Duration(processor.getStartTime(), currentTime);
      if (processingTime.getStandardSeconds() > stuckThreadThresholdSeconds) {
        potentiallyStuck++;
        processor.logPotentiallyStuck(processingTime.getStandardSeconds());
        processor.handlePotentiallyStuck(processingTime);
      }
    }
    return potentiallyStuck;
  }

}
