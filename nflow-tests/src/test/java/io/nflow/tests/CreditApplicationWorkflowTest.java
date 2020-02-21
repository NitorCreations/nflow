package io.nflow.tests;

import static io.nflow.engine.workflow.instance.WorkflowInstanceAction.WorkflowActionType.stateExecution;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static org.apache.cxf.jaxrs.client.WebClient.fromClient;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.joda.time.DateTime.now;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.nflow.rest.v1.msg.Action;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceRequest;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.UpdateWorkflowInstanceRequest;
import io.nflow.tests.demo.workflow.CreditApplicationWorkflow;
import io.nflow.tests.extension.NflowServerConfig;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CreditApplicationWorkflowTest extends AbstractNflowTest {

  public static NflowServerConfig server = new NflowServerConfig.Builder().build();

  public CreditApplicationWorkflowTest() {
    super(server);
  }

  private static CreateWorkflowInstanceRequest req;
  private static CreateWorkflowInstanceResponse resp;

  @Test
  @Order(1)
  public void createCreditApplicationWorkflow() {
    req = new CreateWorkflowInstanceRequest();
    req.type = "creditApplicationProcess";
    req.businessKey = UUID.randomUUID().toString();
    req.stateVariables.put("requestData", (new ObjectMapper()).valueToTree(
            new CreditApplicationWorkflow.CreditApplication("CUST123", new BigDecimal(100l))));
    req.externalId = UUID.randomUUID().toString();
    resp = fromClient(workflowInstanceResource, true).put(req, CreateWorkflowInstanceResponse.class);
    assertThat(resp.id, notNullValue());
  }

  @Test
  @Order(2)
  public void checkAcceptCreditApplicationReached() {
    getWorkflowInstanceWithTimeout(resp.id, "acceptCreditApplication", ofSeconds(10));
  }

  @Test
  @Order(3)
  public void moveToGrantLoanState() {
    UpdateWorkflowInstanceRequest ureq = new UpdateWorkflowInstanceRequest();
    ureq.nextActivationTime = now();
    ureq.state = "grantLoan";
    fromClient(workflowInstanceIdResource, true).path(resp.id).put(ureq);
  }

  @Test
  @Order(4)
  public void checkErrorStateReached() {
    getWorkflowInstanceWithTimeout(resp.id, "error", ofSeconds(5));
  }

  @Test
  @Order(5)
  public void checkWorkflowInstanceActions() {
    int i = 1;
    assertWorkflowInstance(resp.id, actionHistoryValidator(asList(
        new Action(i++, stateExecution.name(), "error", "", 0, null, null, 0),
        new Action(i++, stateExecution.name(), "grantLoan", "", 3, null, null, 0),
        new Action(i++, stateExecution.name(), "grantLoan", "", 2, null, null, 0),
        new Action(i++, stateExecution.name(), "grantLoan", "", 1, null, null, 0),
        new Action(i++, stateExecution.name(), "grantLoan", "", 0, null, null, 0),
        new Action(i++, stateExecution.name(), "grantLoan", "", 0, null, null, 0),
        new Action(i++, stateExecution.name(), "acceptCreditApplication", "", 0, null, null, 0), // probably not the way to show manual action in future
        new Action(i++, stateExecution.name(), "createCreditApplication", "", 0, null, null, 0))));
  }

}
