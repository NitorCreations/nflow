package io.nflow.tests;

import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.util.Arrays.asList;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.runners.MethodSorters.NAME_ASCENDING;

import java.math.BigDecimal;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.joda.time.DateTime;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.CreditApplicationWorkflow;
import io.nflow.tests.runner.NflowServerRule;

@FixMethodOrder(NAME_ASCENDING)
public class PreviewCreditApplicationWorkflowTest extends AbstractNflowTest {

  @ClassRule
  public static NflowServerRule server = new NflowServerRule.Builder().build();

  public PreviewCreditApplicationWorkflowTest() {
    super(server);
  }

  private static CreateWorkflowInstanceRequest req;
  private static CreateWorkflowInstanceResponse resp;
  private static DateTime wfModifiedAtAcceptCreditApplication;

  @Test
  public void t01_createCreditApplicationWorkflow() {
    req = new CreateWorkflowInstanceRequest();
    req.type = "creditApplicationProcess";
    req.startState = CreditApplicationWorkflow.State.previewCreditApplication.toString();
    req.businessKey = UUID.randomUUID().toString();
    req.stateVariables.put("requestData", (new ObjectMapper()).valueToTree(
            new CreditApplicationWorkflow.CreditApplication("CUST123", new BigDecimal(100l))));
    req.externalId = UUID.randomUUID().toString();
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test(timeout = 5000)
  public void t02_checkAcceptCreditApplicationReached() throws InterruptedException {
    ListWorkflowInstanceResponse response;
    do {
      response = getWorkflowInstance(resp.id, "acceptCreditApplication");
    } while (response.nextActivation != null);
    wfModifiedAtAcceptCreditApplication = response.modified;
    assertTrue(response.stateVariables.containsKey("info"));
  }

  @Test
  public void t03_moveToGrantLoanState() {
    UpdateWorkflowInstanceRequest ureq = new UpdateWorkflowInstanceRequest();
    ureq.nextActivationTime = now();
    ureq.state = "grantLoan";
    try (Response response = fromClient(workflowInstanceIdResource, true).path(resp.id).put(ureq)) {
      assertThat(response.getStatusInfo().getFamily(), is(Family.SUCCESSFUL));
    }
  }

  @Test(timeout = 5000)
  public void t04_checkDoneStateReached() throws InterruptedException {
    ListWorkflowInstanceResponse response;
    do {
      response = getWorkflowInstance(resp.id, "done");
    } while (response.nextActivation != null);
    assertTrue("nflow_workflow.modified should be updated", response.modified.isAfter(wfModifiedAtAcceptCreditApplication));
  }

  @Test
  public void t05_checkWorkflowInstanceActions() {
    int i = 1;
    assertWorkflowInstance(resp.id, actionHistoryValidator(asList(
        new Action(i++, stateExecution.name(), "done", "", 0, null, null, 0),
        new Action(i++, stateExecution.name(), "finishCreditApplication", "", 0, null, null, 0),
        new Action(i++, stateExecution.name(), "grantLoan", "", 0, null, null, 0),
        new Action(i++, stateExecution.name(), "grantLoan", "", 0, null, null, 0),
        new Action(i++, stateExecution.name(), "acceptCreditApplication", "", 0, null, null, 0), // probably not the way to show manual action in future
        new Action(i++, stateExecution.name(), "previewCreditApplication", "", 0, null, null, 0))));
  }

}
