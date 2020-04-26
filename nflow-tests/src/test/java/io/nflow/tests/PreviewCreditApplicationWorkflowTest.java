package io.nflow.tests;

import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.joda.time.DateTime;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.ListWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.CreditApplicationWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PreviewCreditApplicationWorkflowTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().build();

  public PreviewCreditApplicationWorkflowTest() {
    super(server);
  }

  private static CreateWorkflowInstanceRequest req;
  private static CreateWorkflowInstanceResponse resp;
  private static DateTime wfModifiedAtAcceptCreditApplication;

  @Test
  @Order(1)
  public void createCreditApplicationWorkflow() {
    req = new CreateWorkflowInstanceRequest();
    req.type = "creditApplicationProcess";
    req.startState = CreditApplicationWorkflow.State.previewCreditApplication.toString();
    req.businessKey = UUID.randomUUID().toString();
    req.stateVariables.put("requestData", (new ObjectMapper()).valueToTree(
            new CreditApplicationWorkflow.CreditApplication("CUST123", new BigDecimal(100l))));
    req.externalId = UUID.randomUUID().toString();
    resp = createWorkflowInstance(req);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void checkAcceptCreditApplicationReached() {
    ListWorkflowInstanceResponse response = getWorkflowInstanceWithTimeout(resp.id, "acceptCreditApplication", ofSeconds(5));
    wfModifiedAtAcceptCreditApplication = response.modified;
    assertTrue(response.stateVariables.containsKey("info"));
  }

  @Test
  @Order(3)
  public void moveToGrantLoanState() {
    UpdateWorkflowInstanceRequest ureq = new UpdateWorkflowInstanceRequest();
    ureq.nextActivationTime = now();
    ureq.state = "grantLoan";
    try (Response response = updateWorkflowInstance(resp.id, ureq, Response.class)) {
      assertThat(response.getStatusInfo().getFamily(), is(Family.SUCCESSFUL));
    }
  }

  @Test
  @Order(4)
  public void checkDoneStateReached() {
    ListWorkflowInstanceResponse response = getWorkflowInstanceWithTimeout(resp.id, "done", ofSeconds(5));
    assertTrue(response.modified.isAfter(wfModifiedAtAcceptCreditApplication), "nflow_workflow.modified should be updated");
  }

  @Test
  @Order(5)
  public void checkWorkflowInstanceActions() {
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
