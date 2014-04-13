package com.nitorcreations.nflow.tests.demo;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nitorcreations.nflow.engine.workflow.StateExecution;

public class WordGeneratorErrorsWorkflow extends WordGeneratorWorkflow {
  private static final Logger log = LoggerFactory
      .getLogger(WordGeneratorErrorsWorkflow.class);
  private static final double ERROR_FRACTION = 0.5;

  public WordGeneratorErrorsWorkflow() {
    super("wordGeneratorErrors");
  }

  @Override
  protected void update(StateExecution execution, String state) {
    Random random = new Random();
    if (random.nextDouble() < ERROR_FRACTION / 2.0) {
      log.info("Generating error at state {} before new state is set", state);
      throw new RuntimeException("error at state " + state
          + " before new state is set");
    }
    super.update(execution, state);
    if (random.nextDouble() < ERROR_FRACTION / 2.0) {
      log.info("Generating error at state {} after new state is set", state);
      throw new RuntimeException("error at state " + state + " after new state is set");
    }
  }

}
