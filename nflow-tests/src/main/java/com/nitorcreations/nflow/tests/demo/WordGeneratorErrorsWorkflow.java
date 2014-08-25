package com.nitorcreations.nflow.tests.demo;

import java.util.Random;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;

public class WordGeneratorErrorsWorkflow extends WordGeneratorWorkflow {
  private static final Logger logger = LoggerFactory
      .getLogger(WordGeneratorErrorsWorkflow.class);
  private static final double ERROR_FRACTION = 0.5;

  public WordGeneratorErrorsWorkflow() {
    super("wordGeneratorErrors", new WordGeneratorErrorsWorkSettings());
  }

  @Override
  protected void update(StateExecution execution, String state) {
    Random random = new Random();
    if (random.nextDouble() < ERROR_FRACTION / 2.0) {
      logger.info("Generating error at state {} before new state is set", state);
      throw new RuntimeException("error at state " + state
          + " before new state is set");
    }
    super.update(execution, state);
    if (random.nextDouble() < ERROR_FRACTION / 2.0) {
      logger.info("Generating error at state {} after new state is set", state);
      throw new RuntimeException("error at state " + state + " after new state is set");
    }
  }

  private static class WordGeneratorErrorsWorkSettings extends WorkflowSettings {
    public WordGeneratorErrorsWorkSettings() {
      super(null);
    }
    @Override
    public DateTime getErrorTransitionActivation() {
      return super.getErrorTransitionActivation();
    }

    @Override
    public int getErrorTransitionDelay() {
      return 300;
    }

    @Override
    public int getShortTransitionDelay() {
      return 200;
    }

    @Override
    public int getImmediateTransitionDelay() {
      return 100;
    }

    @Override
    public int getMaxRetries() {
      return 10;
    }
  }
}
