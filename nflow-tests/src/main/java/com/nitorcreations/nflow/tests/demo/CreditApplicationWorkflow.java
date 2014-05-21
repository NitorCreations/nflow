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

import com.nitorcreations.nflow.engine.workflow.StateExecution;
import com.nitorcreations.nflow.engine.workflow.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.WorkflowStateType;

public class CreditApplicationWorkflow extends WorkflowDefinition<CreditApplicationWorkflow.State> {

  private static final Logger logger = getLogger(CreditApplicationWorkflow.class);

  public static enum State implements com.nitorcreations.nflow.engine.workflow.WorkflowState {
    createCreditApplication(start), acceptCreditApplication(manual), grantLoan(manual),
    finishCreditApplication(normal), done(end), error(manual);

    private WorkflowStateType type;

    private State(WorkflowStateType type) {
      this.type = type;
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
      return name();
    }
  }

  public CreditApplicationWorkflow() {
    super("creditApplicationProcess", createCreditApplication, error);
    permit(createCreditApplication, acceptCreditApplication);
    permit(acceptCreditApplication, grantLoan);
    permit(acceptCreditApplication, finishCreditApplication);
    permit(finishCreditApplication, done);
  }

  public void createCreditApplication(StateExecution execution) {
    logger.info("External service call for creating credit application");
    execution.setNextState(acceptCreditApplication, "Credit application created", now());
  }

  public void acceptCreditApplication(StateExecution execution) {
    logger.info("TODO: should not require method for manual state");
  }

  public void grantLoan(StateExecution execution) {
    logger.info("External service call for granting a loan");
    execution.setNextState(finishCreditApplication, "Loan granted", now());
  }

  public void finishCreditApplication(StateExecution execution) {
    logger.info("External service call for finishing credit application");
    execution.setNextState(done, "Credit application finished", now());
  }

  public void done(StateExecution execution) {
    logger.info("Credit application process ended");
  }

  public void error(StateExecution execution) {
    logger.info("TODO: should not require method for manual state");
  }

  public static class CreditApplication {
    public String customerId;
    public BigDecimal amount;

    public CreditApplication(String customerId, BigDecimal amount) {
      this.customerId = customerId;
      this.amount = amount;
    }
  }

}
