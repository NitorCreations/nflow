package com.nitorcreations.nflow.tests;

import static com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.util.Arrays.asList;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitorcreations.nflow.rest.v1.msg.Action;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow;
import com.nitorcreations.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class CreditApplicationWorkflowTest extends AbstractNflowTest {

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().build();

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

  @Test(timeout = 10000)
  public void t02_checkAcceptCreditApplicationReached() throws InterruptedException {
    ListWorkflowInstanceResponse response;
    do {
      response = getWorkflowInstance(resp.id, "acceptCreditApplication");
    } while (response.nextActivation != null);
  }

  @Test
  public void t03_moveToGrantLoanState() {
    UpdateWorkflowInstanceRequest ureq = new UpdateWorkflowInstanceRequest();
    ureq.nextActivationTime = now();
    ureq.state = "grantLoan";
    fromClient(workflowInstanceResource, true).path(resp.id).put(ureq);
  }

  @Test(timeout = 5000)
  public void t04_checkErrorStateReached() throws InterruptedException {
    ListWorkflowInstanceResponse response;
    do {
      response = getWorkflowInstance(resp.id, "error");
    } while (response.nextActivation != null);
  }

  @Test
  public void t05_checkWorkflowInstanceActions() {
    int i = 1;
    assertWorkflowInstance(resp.id, actionHistoryValidator(asList(
            new Action(i++, stateExecution.name(), "createCreditApplication", "", 0, null, null, 0),
            new Action(i++, stateExecution.name(), "acceptCreditApplication", "", 0, null, null, 0), // probably not the way to show manual action in future
            new Action(i++, stateExecution.name(), "grantLoan", "", 0, null, null, 0),
            new Action(i++, stateExecution.name(), "grantLoan", "", 0, null, null, 0),
            new Action(i++, stateExecution.name(), "grantLoan", "", 1, null, null, 0),
            new Action(i++, stateExecution.name(), "grantLoan", "", 2, null, null, 0),
            new Action(i++, stateExecution.name(), "grantLoan", "", 3, null, null, 0),
            new Action(i++, stateExecution.name(), "error", "", 0, null, null, 0))));
  }

}
