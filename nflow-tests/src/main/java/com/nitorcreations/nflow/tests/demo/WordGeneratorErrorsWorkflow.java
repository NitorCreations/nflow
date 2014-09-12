package com.nitorcreations.nflow.tests.demo;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;

public class WordGeneratorErrorsWorkflow extends WordGeneratorWorkflow {
  private static final Logger logger = LoggerFactory
      .getLogger(WordGeneratorErrorsWorkflow.class);
  private static final double ERROR_FRACTION = 0.5;

  public WordGeneratorErrorsWorkflow() {
    super("wordGeneratorErrors", wordGeneratorErrorsWorkSettings);
  }

  @Override
  protected NextAction update(StateExecution execution, String state) {
    Random random = new Random();
    if (random.nextDouble() < ERROR_FRACTION / 2.0) {
      logger.info("Generating error at state {} before new state is set", state);
      throw new RuntimeException("error at state " + state
          + " before new state is set");
    }
    NextAction nextAction = super.update(execution, state);
    if (random.nextDouble() < ERROR_FRACTION / 2.0) {
      logger.info("Generating error at state {} after new state is set", state);
      throw new RuntimeException("error at state " + state + " after new state is set");
    }
    return nextAction;
  }

  private static final WorkflowSettings wordGeneratorErrorsWorkSettings = new WorkflowSettings.Builder().setMinErrorTransitionDelay(300).setMaxErrorTransitionDelay(1000).setShortTransitionDelay(200).setImmediateTransitionDelay(100).setMaxRetries(10).build();
}
