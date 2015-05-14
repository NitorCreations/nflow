package com.nitorcreations.nflow.tests;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.nitorcreations.nflow.rest.v1.msg.Action;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowDefinitionResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;
import com.nitorcreations.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.WorkflowDefinitionStatisticsResponse;
import com.nitorcreations.nflow.tests.config.PropertiesConfiguration;
import com.nitorcreations.nflow.tests.config.RestClientConfiguration;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;
import com.nitorcreations.nflow.tests.runner.SkipTestMethodsAfterFirstFailureRule;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RestClientConfiguration.class, PropertiesConfiguration.class })
public abstract class AbstractNflowTest {
  protected WebClient workflowInstanceResource;
  protected WebClient workflowDefinitionResource;
  protected WebClient statisticsResource;

  @Rule
  public final SkipTestMethodsAfterFirstFailureRule failFastRule;

  private final NflowServerRule server;

  public AbstractNflowTest(NflowServerRule server) {
    this.server = server;
    this.failFastRule = new SkipTestMethodsAfterFirstFailureRule(getClass());
  }

  @Inject
  public void setWorkflowInstanceResource(@Named("workflowInstance") WebClient client) {
    String newUri = UriBuilder.fromUri(client.getCurrentURI()).port(server.getPort()).build().toString();
    this.workflowInstanceResource = fromClient(client, true).to(newUri, false);
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

  protected ListWorkflowInstanceResponse getWorkflowInstance(int instanceId) {
    return getInstanceResource(instanceId).query("include", "currentStateVariables,actions,actionStateVariables").get(
        ListWorkflowInstanceResponse.class);
  }

  private WebClient getInstanceResource(int instanceId) {
    WebClient client = fromClient(workflowInstanceResource, true).path(Integer.toString(instanceId));
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

  protected ListWorkflowInstanceResponse getWorkflowInstance(int id, String expectedState) throws InterruptedException {
    ListWorkflowInstanceResponse wf = null;
    do {
      sleep(200);
      wf = getWorkflowInstance(id);
    } while (wf == null || !expectedState.equals(wf.state));
    return wf;
  }



  protected void assertWorkflowInstance(int instanceId, WorkflowInstanceValidator... validators) {
    ListWorkflowInstanceResponse instance = getWorkflowInstance(instanceId);
    for (WorkflowInstanceValidator validator : validators) {
      validator.validate(instance);
    }
  }

  protected WorkflowInstanceValidator actionHistoryValidator(final List<Action> actions) {
    return new WorkflowInstanceValidator() {
      @Override
      public void validate(ListWorkflowInstanceResponse workflowInstance) {
        for (int i=0; i<workflowInstance.actions.size(); i++) {
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

  protected String updateWorkflowInstance(int instanceId, UpdateWorkflowInstanceRequest request) {
    return getInstanceResource(instanceId).put(request, String.class);
  }

  private <T> T makeWorkflowInstanceQuery(CreateWorkflowInstanceRequest request, Class<T> responseClass) {
    return fromClient(workflowInstanceResource, true).put(request, responseClass);
  }

  public interface WorkflowInstanceValidator {
    void validate(ListWorkflowInstanceResponse workflowInstance);
  }
}
