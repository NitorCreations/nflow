package com.nitorcreations.nflow.metrics;

import static java.lang.String.format;

import org.springframework.core.env.Environment;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;
import com.nitorcreations.nflow.engine.WorkflowExecutorListener;

/**
 * Compute following metrics on per state basis
 * <ul>
 * <li>Execution time histograms and execution rate</li>
 * <li>Number of successful executions</li>
 * <li>Number of failed executions</li>
 * <li>Retry count histograms</li>
 * </ul>
 */
public class MetricsWorkflowExecutorListener implements
    WorkflowExecutorListener {
  private static final String EXECUTION_KEY = "nflow-metrics-execution";
  private final MetricRegistry metricRegistry;
  private final String nflowInstanceName;

  public MetricsWorkflowExecutorListener(MetricRegistry metricRegistry,
      Environment env) {
    this.metricRegistry = metricRegistry;
    this.nflowInstanceName = env.getRequiredProperty("nflow.instance.name");
  }

  @Override
  public void beforeProcessing(ListenerContext context) {
    @SuppressWarnings("resource")
    Context timerContext = metricRegistry.timer(
        metricKey(context, "execution-time")).time();
    context.data.put(EXECUTION_KEY, timerContext);
    metricRegistry.histogram(metricKey(context, "retries")).update(
        context.stateExecution.getRetries());
  }

  @Override
  public void afterProcessing(ListenerContext context) {
    executionTimer(context).stop();
    metricRegistry.meter(metricKey(context, "success-count")).mark();
  }

  @Override
  public void afterFailure(ListenerContext context, Throwable exeption) {
    @SuppressWarnings("resource")
    Context timer = executionTimer(context);
    if (timer != null) {
      timer.close();
    }

    metricRegistry.meter(metricKey(context, "error-count")).mark();
  }

  private String metricKey(ListenerContext context, String type) {
    String workflowName = context.definition.getName();
    String stateName = context.originalState;
    return format("nflow.%s.%s.%s.%s", nflowInstanceName, workflowName,
        stateName, type);
  }

  private Context executionTimer(ListenerContext context) {
    return (Context) context.data.get(EXECUTION_KEY);
  }
}
