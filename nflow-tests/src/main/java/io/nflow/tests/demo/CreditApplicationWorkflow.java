package io.nflow.tests.demo;

import static io.nflow.engine.workflow.definition.NextAction.moveToState;
import static io.nflow.engine.workflow.definition.WorkflowStateType.end;
import static io.nflow.engine.workflow.definition.WorkflowStateType.manual;
import static io.nflow.engine.workflow.definition.WorkflowStateType.normal;
import static io.nflow.engine.workflow.definition.WorkflowStateType.start;
import static io.nflow.tests.demo.CreditApplicationWorkflow.State.acceptCreditApplication;
import static io.nflow.tests.demo.CreditApplicationWorkflow.State.createCreditApplication;
import static io.nflow.tests.demo.CreditApplicationWorkflow.State.done;
import static io.nflow.tests.demo.CreditApplicationWorkflow.State.error;
import static io.nflow.tests.demo.CreditApplicationWorkflow.State.finishCreditApplication;
import static io.nflow.tests.demo.CreditApplicationWorkflow.State.grantLoan;
import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigDecimal;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.StateVar;
import io.nflow.engine.workflow.definition.WorkflowDefinition;
import io.nflow.engine.workflow.definition.WorkflowSettings;
import io.nflow.engine.workflow.definition.WorkflowStateType;

@SuppressFBWarnings(value="URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD", justification = "jackson reads public fields")
public class CreditApplicationWorkflow extends WorkflowDefinition<CreditApplicationWorkflow.State> {

  private static final Logger logger = getLogger(CreditApplicationWorkflow.class);
  private static final String VAR_KEY = "info";

  public static enum State implements io.nflow.engine.workflow.definition.WorkflowState {
    createCreditApplication(start, "Credit application is persisted to database"),
    previewCreditApplication(start, "Check if credit application would be accepted (ie. simulate)"),
    acceptCreditApplication(manual, "Manual credit decision is made"),
    grantLoan(normal, "Loan is created to loan system"),
    finishCreditApplication(normal, "Credit application status is set"),
    done(end, "Credit application process finished"),
    error(manual, "Manual processing of failed applications");

    private final WorkflowStateType type;
    private final String description;

    private State(WorkflowStateType type, String description) {
      this.type = type;
      this.description = description;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  public CreditApplicationWorkflow() {
    super("creditApplicationProcess", createCreditApplication, error, new WorkflowSettings.Builder().setMinErrorTransitionDelay(0).setMaxErrorTransitionDelay(0).setShortTransitionDelay(0).setMaxRetries(3).build());
    permit(createCreditApplication, acceptCreditApplication);
    permit(acceptCreditApplication, grantLoan);
    permit(acceptCreditApplication, finishCreditApplication);
    permit(finishCreditApplication, done);
  }

  public NextAction createCreditApplication(StateExecution execution, @StateVar(value="requestData", readOnly=true) CreditApplication request, @StateVar(instantiateIfNotExists=true, value=VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: external service call for persisting credit application using request data");
    info.applicationId = "abc" + request.customerId;
    return moveToState(acceptCreditApplication, "Credit application created");
  }

  public NextAction previewCreditApplication(StateExecution execution, @StateVar(value="requestData", readOnly=false) CreditApplication request, @StateVar(instantiateIfNotExists=true, value=VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: external service call for persisting credit application using request data");
    info.applicationId = "abc" + request.customerId;
    request.simulation = true;
    return moveToState(acceptCreditApplication, "Credit application previewed");
  }

  public void acceptCreditApplication(StateExecution execution, @StateVar(value=VAR_KEY) WorkflowInfo info) {
    System.err.println(execution.getVariable("diipa", Boolean.class));
    logger.info("IRL: descheduling workflow instance, next state set externally");
  }

  public NextAction grantLoan(StateExecution execution, @StateVar(value="requestData", readOnly=true) CreditApplication request, @StateVar(value=VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: external service call for granting a loan");
    if (request.simulation) {
      logger.info("STUPID USER");
      return moveToState(finishCreditApplication, "lörläbä");
    }
    throw new RuntimeException("Failed to create loan");
  }

  public NextAction finishCreditApplication(StateExecution execution, @StateVar(value=VAR_KEY) WorkflowInfo info) {
    logger.info("IRL: external service call for updating credit application status");
    return moveToState(done, "Credit application finished");
  }

  public void done(StateExecution execution, @StateVar(value=VAR_KEY) WorkflowInfo info) {
    logger.info("Credit application process ended");
  }

  public void error(StateExecution execution, @StateVar(value=VAR_KEY) WorkflowInfo info) {
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
