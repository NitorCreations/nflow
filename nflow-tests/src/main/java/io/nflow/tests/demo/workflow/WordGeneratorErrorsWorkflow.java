package io.nflow.tests.demo.workflow;

import static org.joda.time.Duration.millis;
import static org.joda.time.Period.days;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.nflow.engine.workflow.definition.NextAction;
import io.nflow.engine.workflow.definition.StateExecution;
import io.nflow.engine.workflow.definition.WorkflowSettings;

public class WordGeneratorErrorsWorkflow extends WordGeneratorWorkflow {
  private static final Logger logger = LoggerFactory.getLogger(WordGeneratorErrorsWorkflow.class);
  private static final double ERROR_FRACTION = 0.5;
  private static final WorkflowSettings wordGeneratorErrorsWorkSettings = new WorkflowSettings.Builder()
      .setMinErrorTransitionDelay(millis(300)).setMaxErrorTransitionDelay(millis(1000)).setShortTransitionDelay(millis(200))
      .setMaxRetries(10).setHistoryDeletableAfter(days(2)).build();

  public WordGeneratorErrorsWorkflow() {
    super("wordGeneratorErrors", wordGeneratorErrorsWorkSettings);
    setDescription("Workflow for testing randomly failing states");
  }

  @Override
  protected NextAction update(StateExecution execution, String state) {
    Random random = ThreadLocalRandom.current();
    if (random.nextDouble() < ERROR_FRACTION / 2.0) {
      logger.info("Generating error at state {} before new state is set", state);
      throw new RuntimeException("error at state " + state + " before new state is set");
    }
    NextAction nextAction = super.update(execution, state);
    if (random.nextDouble() < ERROR_FRACTION / 2.0) {
      logger.info("Generating error at state {} after new state is set", state);
      throw new RuntimeException("error at state " + state + " after new state is set");
    }
    return nextAction;
  }

}
