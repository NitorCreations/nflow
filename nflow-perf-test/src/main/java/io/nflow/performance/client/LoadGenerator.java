package io.nflow.performance.client;

import static java.lang.Integer.getInteger;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.LinkedList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import io.nflow.performance.workflow.NoDelaysWorkflow;
import io.nflow.rest.v1.msg.CreateWorkflowInstanceResponse;
import io.nflow.rest.v1.msg.StatisticsResponse;

/**
 * Starts given number of client threads (client.threads property, default 2) that submit
 * given amount of workflow instances (generated.instance.count, default 2000) to performance tested
 * server.
 */
@Named
public class LoadGenerator {

  private static final Logger logger = LoggerFactory.getLogger(LoadGenerator.class);
  private static final StopWatch elapsedTime = new StopWatch();

  @Inject
  private PerfTestClient client;

  private List<Long> generateSomeLoad(int threadCount, int loadCount) throws InterruptedException {
    List<Long> allInstanceIds = new LinkedList<>();
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

  private static final class LoadGeneratorThread extends Thread {
    private final int id;
    private final PerfTestClient c;
    private final int loadCount;
    private final StopWatch stopWatch = new StopWatch();
    private final List<Long> instanceIds = new LinkedList<>();

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

    public List<Long> getInstanceIds() {
      return instanceIds;
    }
  }

  public void waitForFinish() throws InterruptedException {
    for (int i = 0; i < 600; i++) {
      StatisticsResponse stats = client.getStatistics();
      if (stats.queueStatistics.count == 0) {
        break;
      }
      SECONDS.sleep(1);
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
