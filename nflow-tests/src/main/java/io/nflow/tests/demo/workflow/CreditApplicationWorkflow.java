package io.nflow.tests.demo.workflow;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigDecimal;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.workflow.curated.State;
import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowState;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads public fields")
public class CreditApplicationWorkflow extends AbstractWorkflowDefinition {

  private static final Logger logger = getLogger(CreditApplicationWorkflow.class);
  private static final String VAR_KEY = "info";

  private static final WorkflowState CREATE_CREDIT_APPLICATION = new State("createCreditApplication",
      WorkflowStateType.start, "Credit application is persisted to database");
  public static final WorkflowState PREVIEW_CREDIT_APPLICATION = new State("previewCreditApplication", start,
      "Check if credit application would be accepted (ie. simulate)");
  private static final WorkflowState ACCEPT_CREDIT_APPLICATION = new State("acceptCreditApplication", manual,
      "Manual credit decision is made");
  private static final WorkflowState GRANT_LOAN = new State("grantLoan", "Loan is created to loan system");
  private static final WorkflowState FINISH_CREDIT_APPLICATION = new State("finishCreditApplication",
      "Credit application status is set");
  private static final WorkflowState DONE = new State("done", end, "Credit application process finished");
  private static final WorkflowState ERROR = new State("error", manual, "Manual processing of failed applications");

  public CreditApplicationWorkflow() {
    super("creditApplicationProcess", CREATE_CREDIT_APPLICATION, ERROR, new WorkflowSettings.Builder()
        .setMinErrorTransitionDelay(0).setMaxErrorTransitionDelay(0).setShortTransitionDelay(0).setMaxRetries(3).build());
    setDescription("Mock workflow that makes credit decision, creates loan, deposits the money and updates credit application");
    permit(CREATE_CREDIT_APPLICATION, ACCEPT_CREDIT_APPLICATION);
    permit(ACCEPT_CREDIT_APPLICATION, GRANT_LOAN);
    permit(ACCEPT_CREDIT_APPLICATION, FINISH_CREDIT_APPLICATION);
    permit(FINISH_CREDIT_APPLICATION, DONE);
    registerState(PREVIEW_CREDIT_APPLICATION);
  }

  public NextAction createCreditApplication(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = "requestData", readOnly = true) CreditApplication request,
      @StateVar(instantiateIfNotExists = true, value = VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: external service call for persisting credit application using request data");
    info.applicationId = "abc" + request.customerId;
    return moveToState(ACCEPT_CREDIT_APPLICATION, "Credit application created");
  }

  public NextAction previewCreditApplication(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = "requestData", readOnly = false) CreditApplication request,
      @StateVar(instantiateIfNotExists = true, value = VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: external service call for persisting credit application using request data");
    info.applicationId = "abc" + request.customerId;
    request.simulation = true;
    return moveToState(ACCEPT_CREDIT_APPLICATION, "Credit application previewed");
  }

  public void acceptCreditApplication(StateExecution execution,
      @SuppressWarnings("unused") @StateVar(value = VAR_KEY) WorkflowInfo info) {
    System.err.println(execution.getVariable("diipa", Boolean.class));
    logger.info("IRL: descheduling workflow instance, next state set externally");
  }

  public NextAction grantLoan(@SuppressWarnings("unused") StateExecution execution,
      @StateVar(value = "requestData", readOnly = true) CreditApplication request,
      @SuppressWarnings("unused") @StateVar(value = VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: external service call for granting a loan");
    if (request.simulation) {
      logger.info("STUPID USER");
      return moveToState(FINISH_CREDIT_APPLICATION, "lörläbä");
    }
    throw new RuntimeException("Failed to create loan");
  }

  public NextAction finishCreditApplication(@SuppressWarnings("unused") StateExecution execution,
      @SuppressWarnings("unused") @StateVar(value = VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: external service call for updating credit application status");
    return moveToState(DONE, "Credit application finished");
  }

  public void done(@SuppressWarnings("unused") StateExecution execution,
      @SuppressWarnings("unused") @StateVar(value = VAR_KEY) WorkflowInfo info) {
    logger.info("Credit application process ended");
  }

  public void error(@SuppressWarnings("unused") StateExecution execution,
      @SuppressWarnings("unused") @StateVar(value = VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: some UI should poll for workflows that have reached error state");
  }

  public static class CreditApplication {
    public String customerId;
    public BigDecimal amount;
    public boolean simulation = false;

    public CreditApplication() {}

    public CreditApplication(String customerId, BigDecimal amount) {
      this.customerId = customerId;
      this.amount = amount;
    }
  }

  public static class WorkflowInfo {
    public String applicationId;
  }
}
