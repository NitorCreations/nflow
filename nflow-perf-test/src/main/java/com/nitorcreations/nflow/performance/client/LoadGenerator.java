package com.nitorcreations.nflow.performance.client;

import static java.lang.Integer.getInteger;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.nitorcreations.nflow.performance.workflow.NoDelaysWorkflow;
import com.nitorcreations.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import com.nitorcreations.nflow.rest.v1.msg.StatisticsResponse;

/**
 * Starts given number of client threads (client.threads property, default 2) that submit
 * given amount of workflow instances (generated.instance.count, default 2000) to performance tested
 * server.
 */
@Named
public class LoadGenerator {
  static final Logger logger = LoggerFactory.getLogger(LoadGenerator.class);

  @Inject
  private PerfTestClient client;

  private static final StopWatch elapsedTime = new StopWatch();

  private List<Integer> generateSomeLoad(int threadCount, int loadCount) throws InterruptedException {
    List<Integer> allInstanceIds = new LinkedList<>();
    List<LoadGeneratorThread> threads = new LinkedList<>();
    for (int i = 0; i < threadCount; i++) {
      LoadGeneratorThread t = new LoadGeneratorThread(i, client, loadCount);
      t.start();
      threads.add(t);
    }
    for (LoadGeneratorThread t : threads) {
      t.join();
    }
    for (LoadGeneratorThread t : threads) {
      allInstanceIds.addAll(t.getInstanceIds());
    }
    return allInstanceIds;
  }

  class LoadGeneratorThread extends Thread {
    private final int id;
    private final PerfTestClient c;
    private final int loadCount;
    private final StopWatch stopWatch = new StopWatch();
    private final List<Integer> instanceIds = new LinkedList<>();

    public LoadGeneratorThread(int id, PerfTestClient c, int loadCount) {
      this.id = id;
      this.c = c;
      this.loadCount = loadCount;
    }

    @Override
    public void run() {
      logger.info("Starting items generation {} for {} instances", id, loadCount);
      stopWatch.start();
      for (int i = 0; i < loadCount; i++) {
        CreateWorkflowInstanceResponse resp = c.createWorkflow(new NoDelaysWorkflow().getType());
        instanceIds.add(resp.id);
      }
      logger.info("Generated {} items took {} msec for {}", loadCount, stopWatch.getTime(), id);
    }

    public List<Integer> getInstanceIds() {
      return instanceIds;
    }
  }

  public void waitForFinish() throws InterruptedException {
    for (int i = 0; i < 600; i++) {
      StatisticsResponse stats = client.getStatistics();
      if (stats.queueStatistics.count == 0) {
        break;
      }
      sleep(SECONDS.toMillis(1));
    }
  }

  public static void main(String[] args) throws Exception {
    logger.info("Starting");
    try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(PerfTestConfiguration.class)) {
      LoadGenerator loadGenerator = ctx.getBean(LoadGenerator.class);
      elapsedTime.start();
      loadGenerator.generateSomeLoad(getInteger("client.threads", 2), getInteger("generated.instance.count", 2000));
      loadGenerator.waitForFinish();
      elapsedTime.stop();
      logger.info("Finished processing took {} msec", elapsedTime.getTime());
    }
    logger.info("The end");
  }
}
