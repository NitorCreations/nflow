package com.nitorcreations.nflow.tests.demo;

import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.end;
import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.manual;
import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.normal;
import static com.nitorcreations.nflow.engine.workflow.WorkflowStateType.start;
import static com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow.State.acceptCreditApplication;
import static com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow.State.createCreditApplication;
import static com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow.State.done;
import static com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow.State.error;
import static com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow.State.finishCreditApplication;
import static com.nitorcreations.nflow.tests.demo.CreditApplicationWorkflow.State.grantLoan;
import static org.joda.time.DateTime.now;
import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.springframework.core.env.Environment;

import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;

public class CreditApplicationWorkflow extends WorkflowDefinition<CreditApplicationWorkflow.State> {

  private static final Logger logger = getLogger(CreditApplicationWorkflow.class);

  public static enum State implements com.nitorcreations.nflow.engine.workflow.WorkflowState {
    createCreditApplication(start, "Credit application is persisted to database"),
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
    public String getName() {
      return name();
    }

    @Override
    public String getDescription() {
      return description;
    }
  }

  public CreditApplicationWorkflow() {
    super("creditApplicationProcess", createCreditApplication, error, new CreditApplicationWorkflowSettings());
    permit(createCreditApplication, acceptCreditApplication);
    permit(acceptCreditApplication, grantLoan);
    permit(acceptCreditApplication, finishCreditApplication);
    permit(finishCreditApplication, done);
  }

  public void createCreditApplication(StateExecution execution) {
    logger.info("IRL: external service call for persisting credit application using execution.requestData");
    execution.setNextState(acceptCreditApplication, "Credit application created", now());
  }

  public void acceptCreditApplication(StateExecution execution) {
    logger.info("IRL: descheduling workflow instance, next state set externally");
    execution.setNextState(acceptCreditApplication, "Expecting manual credit decision", null);
  }

  public void grantLoan(StateExecution execution) {
    logger.info("IRL: external service call for granting a loan");
    throw new RuntimeException("Failed to create loan");
  }

  public void finishCreditApplication(StateExecution execution) {
    logger.info("IRL: external service call for updating credit application status");
    execution.setNextState(done, "Credit application finished", now());
  }

  public void done(StateExecution execution) {
    logger.info("Credit application process ended");
    execution.setNextState(done);
    execution.setSaveTrace(false);
  }

  public void error(StateExecution execution) {
    logger.info("IRL: some UI should poll for workflows that have reached error state");
    execution.setNextState(error);
  }

  public static class CreditApplication {
    public String customerId;
    public BigDecimal amount;

    public CreditApplication(String customerId, BigDecimal amount) {
      this.customerId = customerId;
      this.amount = amount;
    }
  }

  public static class CreditApplicationWorkflowSettings extends WorkflowSettings {

    public CreditApplicationWorkflowSettings() {
      this(null);
    }

    public CreditApplicationWorkflowSettings(Environment env) {
      super(env);
    }

    @Override
    public int getErrorTransitionDelay() {
      return 0;
    }

    @Override
    public int getShortTransitionDelay() {
      return 0;
    }
  }

}
