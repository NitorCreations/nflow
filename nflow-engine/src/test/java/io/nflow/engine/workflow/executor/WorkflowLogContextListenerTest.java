package io.nflow.engine.workflow.executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.MDC;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import io.nflow.engine.listener.ListenerChain;
import io.nflow.engine.listener.WorkflowExecutorListener.ListenerContext;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.instance.WorkflowInstance;

public class WorkflowLogContextListenerTest {

  private static final String LOG_CONTEXT = "context";

  private final WorkflowLogContextListener listener = new WorkflowLogContextListener(LOG_CONTEXT);

  @Mock
  private AbstractWorkflowDefinition definition;

  @Mock
  private StateExecution stateExecution;

  @AfterAll
  static void afterAll() {
    MDC.clear();
  }

  @Test
  void beforeProcessingWithStateVariables() {
    MDC.clear();
    Map<String, String> vars = new HashMap<>();
    vars.put("foo", "bar");
    Map<String, String> stateVariables = spy(vars);
    WorkflowInstance instance = new WorkflowInstance.Builder().setType("type").setId(1).setExternalId("extId")
        .setBusinessKey("businessKey").setStateVariables(stateVariables).build();
    ListenerContext context = new ListenerContext(definition, instance, stateExecution);

    listener.beforeProcessing(context);

    assertThat(MDC.get(LOG_CONTEXT), is(equalTo("type:type, instanceId:1, extId:extId, businessKey:businessKey")));
    verify(stateVariables).entrySet();
  }

  @Test
  void beforeProcessingWithoutStateVariables() {
    MDC.clear();
    Map<String, String> stateVariables = spy(new HashMap<String, String>());
    WorkflowInstance instance = new WorkflowInstance.Builder().setType("type").setId(1).setExternalId("extId")
        .setBusinessKey("businessKey").setStateVariables(stateVariables).build();
    ListenerContext context = new ListenerContext(definition, instance, stateExecution);

    listener.beforeProcessing(context);

    assertThat(MDC.get(LOG_CONTEXT), is(equalTo("type:type, instanceId:1, extId:extId, businessKey:businessKey")));
    verify(stateVariables, never()).entrySet();
  }

  @Test
  void afterProcessingClearsContext() {
    MDC.put(LOG_CONTEXT, "foo");

    listener.afterProcessing(mock(ListenerContext.class));

    assertThat(MDC.get(LOG_CONTEXT), is(nullValue()));
  }

  @Test
  void afterFailureClearsContext() {
    MDC.put(LOG_CONTEXT, "foo");

    listener.afterFailure(mock(ListenerContext.class), mock(Throwable.class));

    assertThat(MDC.get(LOG_CONTEXT), is(nullValue()));
  }

  @Test
  void processCallsNextInChain() {
    ListenerContext context = mock(ListenerContext.class);
    ListenerChain chain = mock(ListenerChain.class);

    listener.process(context, chain);

    verify(chain).next(context);
  }
}
