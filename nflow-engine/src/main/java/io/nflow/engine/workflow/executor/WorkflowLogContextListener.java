package io.nflow.engine.workflow.executor;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;
import org.slf4j.MDC;

import io.nflow.engine.listener.WorkflowExecutorListener;
import io.nflow.engine.workflow.instance.WorkflowInstance;

public class WorkflowLogContextListener implements WorkflowExecutorListener {

  private static final Logger LOG = getLogger(WorkflowLogContextListener.class);
  private static final String LOG_CONTEXT_FORMAT = "type:%s, instanceId:%s, extId:%s, businessKey:%s";
  private final String logContext;

  public WorkflowLogContextListener(String logContext) {
    this.logContext = logContext;
  }

  @Override
  public void beforeProcessing(ListenerContext listenerContext) {
    WorkflowInstance instance = listenerContext.instance;
    MDC.put(logContext, format(LOG_CONTEXT_FORMAT, instance.type, instance.id, instance.externalId, instance.businessKey));
    if (LOG.isDebugEnabled() && !instance.stateVariables.isEmpty()) {
      LOG.debug("State variables:\n{}", instance.stateVariables.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).collect(joining("\n")));
    }
  }

  @Override
  public void afterProcessing(ListenerContext listenerContext) {
    MDC.remove(logContext);
  }

  @Override
  public void afterFailure(ListenerContext listenerContext, Throwable throwable) {
    MDC.remove(logContext);
  }

}
