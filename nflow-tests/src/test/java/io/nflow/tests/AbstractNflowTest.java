package io.nflow.tests;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.SetSignalRequest;
import io.nflow.rest.v1.msg.StatisticsResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.WakeupRequest;
import io.nflow.rest.v1.msg.WakeupResponse;
import io.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import io.nflow.tests.config.PropertiesConfiguration;
import io.nflow.tests.config.RestClientConfiguration;
import io.nflow.tests.extension.NflowServerConfig;
import io.nflow.tests.extension.SkipTestMethodsAfterFirstFailureExtension;

@ExtendWith({ SpringExtension.class, SkipTestMethodsAfterFirstFailureExtension.class })
@ContextConfiguration(classes = { RestClientConfiguration.class, PropertiesConfiguration.class })
public abstract class AbstractNflowTest {
  protected WebClient workflowInstanceResource;
  protected WebClient workflowInstanceIdResource;
  protected WebClient workflowDefinitionResource;
  protected WebClient statisticsResource;

  private final NflowServerConfig server;

  public AbstractNflowTest(NflowServerConfig server) {
    this.server = server;
  }

  @Inject
  public void setWorkflowInstanceResource(@Named("workflowInstance") WebClient client) {
    String newUri = UriBuilder.fromUri(client.getCurrentURI()).port(server.getPort()).build().toString();
    this.workflowInstanceResource = fromClient(client, true).to(newUri, false);
  }

  @Inject
  public void setWorkflowInstanceIdResource(@Named("workflowInstanceId") WebClient client) {
    String newUri = UriBuilder.fromUri(client.getCurrentURI()).port(server.getPort()).build().toString();
    this.workflowInstanceIdResource = fromClient(client, true).to(newUri, false);
  }

  @Inject
  public void setWorkflowDefinitionResource(@Named("workflowDefinition") WebClient client) {
    String newUri = UriBuilder.fromUri(client.getCurrentURI()).port(server.getPort()).build().toString();
    this.workflowDefinitionResource = fromClient(client, true).to(newUri, false);
  }

  @Inject
  public void setStatisticsResource(@Named("statistics") WebClient client) {
    String newUri = UriBuilder.fromUri(client.getCurrentURI()).port(server.getPort()).build().toString();
    this.statisticsResource = fromClient(client, true).to(newUri, false);
  }

  protected ListWorkflowInstanceResponse getWorkflowInstance(long instanceId) {
    return getInstanceIdResource(instanceId).query("include", "currentStateVariables,actions,actionStateVariables,childWorkflows")
        .get(ListWorkflowInstanceResponse.class);
  }

  protected WakeupResponse wakeup(long instanceId, List<String> expectedStates) {
    WakeupRequest request = new WakeupRequest();
    request.expectedStates = expectedStates;
    return getInstanceResource(instanceId).path("wakeup").put(request, WakeupResponse.class);
  }

  protected String setSignal(long instanceId, int signal, String reason) {
    SetSignalRequest request = new SetSignalRequest();
    request.signal = signal;
    request.reason = reason;
    return getInstanceResource(instanceId).path("signal").put(request, String.class);
  }

  private WebClient getInstanceResource(long instanceId) {
    WebClient client = fromClient(workflowInstanceResource, true).path(Long.toString(instanceId));
    return client;
  }

  protected WebClient getInstanceIdResource(long instanceId) {
    WebClient client = fromClient(workflowInstanceIdResource, true).path(Long.toString(instanceId));
    return client;
  }

  protected ListWorkflowDefinitionResponse[] getWorkflowDefinitions() {
    WebClient client = fromClient(workflowDefinitionResource, true);
    return client.get(ListWorkflowDefinitionResponse[].class);
  }

  public StatisticsResponse getStatistics() {
    WebClient client = fromClient(statisticsResource, true);
    return client.get(StatisticsResponse.class);
  }

  public WorkflowDefinitionStatisticsResponse getDefinitionStatistics(String definitionType) {
    WebClient client = fromClient(statisticsResource, true).path("workflow").path(definitionType);
    return client.get(WorkflowDefinitionStatisticsResponse.class);
  }

  protected ListWorkflowInstanceResponse getWorkflowInstance(long id, String expectedState) throws InterruptedException {
    ListWorkflowInstanceResponse wf = null;
    do {
      sleep(200);
      wf = getWorkflowInstance(id);
    } while (wf == null || !expectedState.equals(wf.state));
    return wf;
  }

  protected ListWorkflowInstanceResponse getWorkflowInstanceWithTimeout(long id, String expectedState, Duration timeout) {
    return assertTimeoutPreemptively(timeout, () -> {
      ListWorkflowInstanceResponse resp;
      do {
        resp = getWorkflowInstance(id, expectedState);
      } while (resp.nextActivation != null);
      return resp;
    });
  }

  protected void assertWorkflowInstance(long instanceId, WorkflowInstanceValidator... validators) {
    ListWorkflowInstanceResponse instance = getWorkflowInstance(instanceId);
    for (WorkflowInstanceValidator validator : validators) {
      validator.validate(instance);
    }
  }

  protected WorkflowInstanceValidator actionHistoryValidator(final List<Action> actions) {
    return new WorkflowInstanceValidator() {
      @Override
      public void validate(ListWorkflowInstanceResponse workflowInstance) {
        for (int i = 0; i < workflowInstance.actions.size(); i++) {
          assertThat("State " + i + " wrong state name", workflowInstance.actions.get(i).state, is(actions.get(i).state));
          assertThat("State " + i + " wrong retry no", workflowInstance.actions.get(i).retryNo, is(actions.get(i).retryNo));
        }
      }
    };
  }

  protected CreateWorkflowInstanceResponse createWorkflowInstance(CreateWorkflowInstanceRequest request) {
    return makeWorkflowInstanceQuery(request, CreateWorkflowInstanceResponse.class);
  }

  protected ObjectMapper nflowObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(NON_EMPTY);
    mapper.registerModule(new JodaModule());
    return mapper;
  }

  protected String updateWorkflowInstance(long instanceId, UpdateWorkflowInstanceRequest request) {
    return getInstanceIdResource(instanceId).put(request, String.class);
  }

  private <T> T makeWorkflowInstanceQuery(CreateWorkflowInstanceRequest request, Class<T> responseClass) {
    return fromClient(workflowInstanceResource, true).put(request, responseClass);
  }

  public interface WorkflowInstanceValidator {
    void validate(ListWorkflowInstanceResponse workflowInstance);
  }
}
