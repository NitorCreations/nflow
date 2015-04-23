package com.nitorcreations.nflow.performance.testdata;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.nitorcreations.nflow.engine.workflow.instance.WorkflowInstance;

/**
 * Manages test data creation through TestDataGenerator and TestDataBatchInserter. TODO: restart serial sequences after
 * performance test data generation for nflow_workflow.id and nflow_workflow_action.id.
 */
public class TestDataManager {

  private final ExecutorService executors;

  public TestDataManager(TestDataGenerator generator, TestDataBatchInserter inserter, Environment env) {
    executors = Executors.newSingleThreadExecutor();
    executors.submit(new PopulatorRunnable(generator, inserter, env));
  }

  static class PopulatorRunnable implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PopulatorRunnable.class);
    private final TestDataGenerator generator;
    private final TestDataBatchInserter inserter;
    private final Environment env;

    public PopulatorRunnable(TestDataGenerator generator, TestDataBatchInserter inserter, Environment env) {
      this.generator = generator;
      this.inserter = inserter;
      this.env = env;
    }

    @Override
    public void run() {
      try {
        generator.setCurrentWorkflowId(inserter.getMaxValueFromColumn("nflow_workflow", "id"));
        generator.setCurrentActionId(inserter.getMaxValueFromColumn("nflow_workflow_action", "id"));
        int batchSize = env.getProperty("testdata.batch.size", Integer.class, 1000);
        int targetCount = env.getProperty("testdata.target.count", Integer.class, 100000);
        int nextBatchSize, generatedCount = 0;
        while ((nextBatchSize = calculateNextBatch(generatedCount, targetCount, batchSize)) > 0) {
          List<WorkflowInstance> instances = generator.generateWorkflowInstances(nextBatchSize);
          inserter.batchInsert(instances);
          generatedCount += nextBatchSize;
          logger.info("Stored {} workflow instances", generatedCount);
        }
        logger.info("Finished");
      } catch (Exception ex) {
        logger.error("Failed to generate instances", ex);
      }
    }

    private int calculateNextBatch(int generatedCount, int targetCount, int batchSize) {
      if (targetCount - generatedCount < batchSize) {
        return targetCount - generatedCount;
      }
      return batchSize;
    }
  }

}
