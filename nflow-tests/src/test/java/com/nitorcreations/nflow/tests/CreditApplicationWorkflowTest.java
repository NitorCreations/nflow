package com.nitorcreations.nflow.tests;

import static java.lang.Thread.sleep;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v0.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v0.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v0.msg.UpdateWorkflowInstanceRequest;
import com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRulue;

@FixMethodOrder(NAME_ASCENDING)
public class CreditApplicationWorkflowTest extends AbstractNflowTest {

  @ClassRule
  public static NflowServerRulue server = new NflowServerRulue().port(7502);

  public CreditApplicationWorkflowTest() {
    super(server);
  }

  private static CreateWorkflowInstanceRequest req;
  private static CreateWorkflowInstanceResponse resp;

  @Test
  public void t01_createCreditApplicationWorkflow() {
    req = new CreateWorkflowInstanceRequest();
    req.type = "creditApplicationProcess";
    req.businessKey = UUID.randomUUID().toString();
    req.requestData = (new ObjectMapper()).valueToTree(
            new CreditApplicationWorkflow.CreditApplication("CUST123", new BigDecimal(100l)));
    req.externalId = UUID.randomUUID().toString();
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  public void t02_checkAcceptCreditApplicationReached() throws InterruptedException {
    ListWorkflowInstanceResponse response = getWorkflowInstance(req.externalId, "acceptCreditApplication");
    assertThat(response.state, is("acceptCreditApplication"));
    assertThat(response.nextActivation, nullValue());
  }

  @Test
  public void t03_moveToGrantLoanState() {
    UpdateWorkflowInstanceRequest ureq = new UpdateWorkflowInstanceRequest();
    ureq.nextActivationTime = now();
    ureq.state = "grantLoan";
    fromClient(workflowInstanceResource, true).path(resp.id).put(ureq);
  }

  @Test
  public void t04_checkErrorStateReached() throws InterruptedException {
    ListWorkflowInstanceResponse response = getWorkflowInstance(req.externalId, "error");
    assertThat(response.state, is("error"));
    assertThat(response.nextActivation, nullValue());
  }

  // TODO: replace with id query when /v0/workflow-instance/{id} exists
  private ListWorkflowInstanceResponse getWorkflowInstance(String externalId, String expectedState) throws InterruptedException {
    ListWorkflowInstanceResponse[] tmp = new ListWorkflowInstanceResponse[0];
    for (int i=0; i<10; i++) {
      tmp = fromClient(workflowInstanceResource, true).query("externalId", req.externalId).get(ListWorkflowInstanceResponse[].class);
      if (tmp.length == 0 || !expectedState.equals(tmp[0].state)) {
        sleep(1000l);
      } else {
        break;
      }
    }
    assertThat(tmp.length, is(1));
    return tmp[0];
  }

}
