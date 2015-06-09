package com.nitorcreations.nflow.metrics;

import static java.lang.String.format;

import org.joda.time.DateTime;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer.Context;
import com.nitorcreations.nflow.engine.internal.dao.ExecutorDao;
import com.nitorcreations.nflow.engine.listener.AbstractWorkflowExecutorListener;

/**
 * Compute following metrics on per state basis
 * <ul>
 * <li>Execution time histograms and execution rate</li>
 * <li>Number of successful executions</li>
 * <li>Number of failed executions</li>
 * <li>Retry count histograms</li>
 * </ul>
 */
public class MetricsWorkflowExecutorListener extends
        AbstractWorkflowExecutorListener {
  private static final String EXECUTION_KEY = "nflow-metrics-execution";
  private final MetricRegistry metricRegistry;
  private final String nflowExecutorGroup;
  private final int nflowExecutorId;

  public MetricsWorkflowExecutorListener(MetricRegistry metricRegistry, ExecutorDao executors) {
    this.metricRegistry = metricRegistry;
    this.nflowExecutorGroup = executors.getExecutorGroup();
    this.nflowExecutorId = executors.getExecutorId();
  }

  @Override
  public void beforeProcessing(ListenerContext context) {
    @SuppressWarnings("resource")
    Context timerContext = metricRegistry.timer(
        stateMetricKey(context, "execution-time")).time();
    context.data.put(EXECUTION_KEY, timerContext);
    meterRetries(context);
    meterStartupDelay(context);
  }

  private void meterRetries(ListenerContext context) {
    metricRegistry.histogram(stateMetricKey(context, "retries")).update(
        context.stateExecution.getRetries());
  }

  private void meterStartupDelay(ListenerContext context) {
    if(context.instance.nextActivation != null) {
      long delay = DateTime.now().getMillis() - context.instance.nextActivation.getMillis();
      metricRegistry.histogram(groupNameMetricKey("startup-delay")).update(delay);
    }
  }

  @Override
  public void afterProcessing(ListenerContext context) {
    executionTimer(context).stop();
    metricRegistry.meter(stateMetricKey(context, "success-count")).mark();
  }

  @Override
  public void afterFailure(ListenerContext context, Throwable exeption) {
    @SuppressWarnings("resource")
    Context timer = executionTimer(context);
    if (timer != null) {
      timer.close();
    }

    metricRegistry.meter(stateMetricKey(context, "error-count")).mark();
  }

  private String stateMetricKey(ListenerContext context, String type) {
    String workflowName = context.definition.getType();
    String stateName = context.originalState;
    return format("%s.%s.%s.%s.%s", nflowExecutorGroup, nflowExecutorId, workflowName,
        stateName, type);
  }

  private String groupNameMetricKey(String type) {
    return format("%s.%s.%s", nflowExecutorGroup, nflowExecutorId, type);
  }

  private Context executionTimer(ListenerContext context) {
    return (Context) context.data.get(EXECUTION_KEY);
  }
}
