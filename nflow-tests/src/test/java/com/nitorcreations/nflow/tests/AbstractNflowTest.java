package com.nitorcreations.nflow.tests;

import static org.apache.cxf.jaxrs.client.WebClient.fromClient;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.UriBuilder;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.nitorcreations.nflow.tests.config.PropertiesConfiguration;
import com.nitorcreations.nflow.tests.config.RestClientConfiguration;
import com.nitorcreations.nflow.tests.runner.NflowServerRulue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RestClientConfiguration.class, PropertiesConfiguration.class })
public abstract class AbstractNflowTest {
  protected WebClient workflowInstanceResource;

  private final NflowServerRulue server;

  public AbstractNflowTest(NflowServerRulue server) {
    this.server = server;
  }

  @Inject
  public void setWorkflowInstanceResource(@Named("workflow-instance") WebClient client) {
    String newUri = UriBuilder.fromUri(client.getCurrentURI()).port(server.getPort()).build().toString();
    this.workflowInstanceResource = fromClient(client, true).to(newUri, false);
  }
}
