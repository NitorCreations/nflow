package com.nitorcreations.nflow.tests;

import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.rest.v0.msg.Action;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.tests.config.PropertiesConfiguration;
import com.nitorcreations.nflow.tests.config.RestClientConfiguration;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RestClientConfiguration.class, PropertiesConfiguration.class })
public abstract class AbstractNflowTest {
  protected WebClient workflowInstanceResource;

  private final NflowServerRule server;

  public AbstractNflowTest(NflowServerRule server) {
    this.server = server;
  }

  @Inject
  public void setWorkflowInstanceResource(@Named("workflowInstance") WebClient client) {
    String newUri = UriBuilder.fromUri(client.getCurrentURI()).port(server.getPort()).build().toString();
    this.workflowInstanceResource = fromClient(client, true).to(newUri, false);
  }

  protected ListWorkflowInstanceResponse getWorkflowInstance(int instanceId) {
    return fromClient(workflowInstanceResource, true).path(Integer.toString(instanceId))
          .query("include", "actions").get(ListWorkflowInstanceResponse.class);
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

  public interface WorkflowInstanceValidator {
    void validate(ListWorkflowInstanceResponse workflowInstance);
  }
}
